package adc.levelreader.main;

import adc.utils.EscapeSeq;

import fona.arduino.ReadWriteFONA;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

import org.fusesource.jansi.AnsiConsole;

import adc.levelreader.manager.AirWaterOilInterface;
import adc.levelreader.manager.PushButtonObserver;
import adc.levelreader.manager.SevenADCChannelsManager;

import com.pi4j.io.gpio.GpioController;

import com.pi4j.io.gpio.GpioFactory;

import com.pi4j.io.gpio.GpioPinDigitalInput;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListener;

import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import fona.arduino.FONAClient;

import java.io.BufferedWriter;
import java.io.FileInputStream;

import java.io.FileWriter;
import java.io.IOException;

import java.net.URI;

import java.util.Properties;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import org.json.JSONObject;

import relay.RelayManager;

/**
 * Relies on props.properties
 * 
 * Synopsis:
 * =========
 * 1. There is water in the bilge, with oil on top.
 * 2. The bild pump starts.
 * 3. When the oil is about to reach the pump, the power of the pump is shut off, and a message (SMS) 
 *    is sent on the phone of the captain, saying "You have X inches of oil in the bilge, you have 24 hours to clean it, 
 *    reply 'CLEAN' to this message from your phone to reset the process and restart your bilge pump".
 * 4. A 'CLEAN' message is received (from captain, owner, or authorities).
 * 5. If the bilge is clean, the process is reset (bilge pump power turned back on).
 * 6. If not, a new message is sent to the captain, the processis NOT reset. 
 * 
 * 7. If the bilge has not been cleaned within a given amount of time (24h by default), 
 *    then another message is sent to the boat owner.
 * 8. After the same amount of time, if the bilge is still no clean, then a message is sent 
 *    to the authorities (Harbor Master, Coast Guards)
 * 
 * Interfaced with:
 * - a bilge probe (ADC)
 * - a FONA (SMS shield)
 * - a WebSocket server (node.js)
 *       also provides a web interface
 */
public class LelandPrototype implements AirWaterOilInterface, FONAClient, PushButtonObserver
{
  private final static boolean ansiConsole = true;
  private final static String LOG_FILE = "log.log";
  
  private static BufferedWriter fileLogger = null;
  
  private static Properties props = null;
  
  private static long cleaningDelay = 0L;  
  
  private static LevelMaterial<Float, SevenADCChannelsManager.Material>[] data = null;
  private final static NumberFormat DF31 = new DecimalFormat("000.0");
  private final static NumberFormat DF4  = new DecimalFormat("###0");
  private static WebSocketClient webSocketClient = null;
  private static ReadWriteFONA smsProvider = null;
  private static RelayManager rm = null;
  private static Pin RESET_PI = RaspiPin.GPIO_10; // GPIO_10, CE0, #24
  
  private static final GpioController gpio = GpioFactory.getInstance();;
  
  private static String wsUri = "";
  private static String phoneNumber_1 = "", 
                        phoneNumber_2 = "",
                        phoneNumber_3 = "";
  private static String boatName = "";
  
  private static boolean fonaReady = false;
  
  private final static int _ALL_OK             = -1;
  private final static int SENT_TO_CAPTAIN     =  0;
  private final static int SENT_TO_OWNER       =  1;
  private final static int SENT_TO_AUTHORITIES =  2;

  public enum ProcessStatus
  {
    ALL_OK(_ALL_OK),
    MESSAGE_SENT_TO_CAPTAIN(SENT_TO_CAPTAIN),
    MESSAGE_SENT_TO_OWNER(SENT_TO_OWNER),
    MESSAGE_SENT_TO_AUTHORITIES(SENT_TO_AUTHORITIES);
    
    private int level;
    private ProcessStatus(int level)
    {
      this.level = level;
    }
    public int level() { return this.level; }
  }
    
  private static ProcessStatus currentStatus = ProcessStatus.ALL_OK;
  
  private static int currentWaterLevel   = 0;
  private static int currentOilThickness = 0;
  
  private static FONAClient fonaClient = null;
  public final static String SIMULATOR = "Simulator";
  
  public LelandPrototype()
  {
    fonaClient = this;
    data = new LevelMaterial[7];
    for (int i=0; i<data.length; i++)
    {
      data[i] = new LevelMaterial<Float, SevenADCChannelsManager.Material>(0f, SevenADCChannelsManager.Material.UNKNOWN);
    }
    
    final GpioPinDigitalInput resetButton = gpio.provisionDigitalInputPin(RESET_PI, PinPullResistance.PULL_DOWN);
    resetButton.addListener(new GpioPinListenerDigital() {
      @Override
      public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        if (event.getState().isHigh())
          onButtonPressed();
      }
    });
  }
  
  @Override
  public void onButtonPressed()
  {
    log(">>> Reset button has been pressed.");
    RelayManager.RelayState status = rm.getStatus("00");
    // log("Relay is:" + status);
    if (RelayManager.RelayState.OFF.equals(status))
    {
      log("Turning relay back on.");
      try { rm.set("00", RelayManager.RelayState.ON); }
      catch (Exception ex)
      {
        System.err.println(ex.toString());
      }
    }

  }

    
  private static void initWebSocketConnection(String serverURI)
  {
    try
    {
      webSocketClient = new WebSocketClient(new URI(serverURI))
      {
        @Override
        public void onOpen(ServerHandshake serverHandshake)
        {
          log("WS On Open");
        }

        @Override
        public void onMessage(String string)
        {
  //        log("WS On Message");
          if (smsProvider == null) // Allow simulated CLEAN message
          {
        //  log("Received [" + string + "]");
            // "{"type":"message","data":{"time":1441877164577,"text":"CLEAN"}}"
            try
            {
              JSONObject json = new JSONObject(string);
              if ("message".equals(json.getString("type")))
              {
                JSONObject data = json.getJSONObject("data");
                if (data != null)
                {
                  if ("CLEAN".equals(data.getString("text")))
                  {
                    fonaClient.message(new ReadWriteFONA.SMS(0, SIMULATOR, "CLEAN".length(), "CLEAN"));
                  }
                }
              }
            }
            catch (Exception ex)
            {
              ex.printStackTrace();
            }
          }
        }

        @Override
        public void onClose(int i, String string, boolean b)
        {
          log("WS On Close");
        }

        @Override
        public void onError(Exception exception)
        {
          log("WS On Error");
          displayAppErr(exception);
  //      exception.printStackTrace();
        }
      }; 
      webSocketClient.connect();
    }
    catch (Exception ex)
    {
      displayAppErr(ex);
  //  ex.printStackTrace();
    }    
  }

  private static String lpad(String str, String with, int len)
  {
    String s = str;
    while (s.length() < len)
      s = with + s;
    return s;
  }
  
  private static String rpad(String str, String with, int len)
  {
    String s = str;
    while (s.length() < len)
      s += with;
    return s;
  }
  
  private static String materialToString(SevenADCChannelsManager.Material material)
  {
    String s = "UNKNOWN";
    if (material == SevenADCChannelsManager.Material.AIR)
      s = "Air";
    else if (material == SevenADCChannelsManager.Material.WATER)
      s = "Water";
    else if (material == SevenADCChannelsManager.Material.OIL)
      s = "Oil";
    return s;
  }

  private static void sendSMS(final String to,
                              final String[] content)
  {
    Thread bg = new Thread()
    {
      public void run()
      {
        for (String s : content)
        {
          sendSMS(to, s);
        }
      }
    };
    bg.start();
  }
  
  private static Thread sendMessWaiter = null;
  
  private static void sendSMS(String to,
                              String content)
  {
    if (smsProvider != null)
    {
      String mess = content;
      if (mess.length() > 140)
        mess = mess.substring(0, 140);
      log(">>> Sending SMS :" + mess);
      smsProvider.sendMess(to, mess);
      sendMessWaiter = Thread.currentThread();
      synchronized (sendMessWaiter)
      {
        try 
        {
          sendMessWaiter.wait(5000L);
        }
        catch (InterruptedException ie)
        {
          ie.printStackTrace();
        }
        log("...Released!");
      }
      sendMessWaiter = null;
    }
    else
      log(">>> Simulating call to " + to + ", " + content);
  }
  
  // User Interface ... Sovietic! And business logic.
  private static void manageData()
  {
    int maxWaterLevel = -1;
    int maxOilLevel   = -1;
    // Clear the screen, cursor on top left.
    String str = "";
    if (ansiConsole)
    {
   // AnsiConsole.out.println(EscapeSeq.ANSI_CLS); 
      AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 1));
      str =        "+---+--------+---------+";
      AnsiConsole.out.println(str);
      str =        "| C |  Vol % |   Mat   |";
      AnsiConsole.out.println(str);
      
      str =        "+---+--------+---------+";
      AnsiConsole.out.println(str);
    }
    for (int chan=data.length - 1; chan >= 0; chan--) // Top to bottom
    {
      str = "| " + Integer.toString(chan) + " | " +
                   lpad(DF4.format(data[chan].getPercent()), " ", 4) + " % | " +
                   lpad(materialToString(data[chan].getMaterial()), " ", 7) + " |";
      if (maxOilLevel == -1 && data[chan].getMaterial().equals(SevenADCChannelsManager.Material.OIL))
        maxOilLevel = chan;
      if (maxWaterLevel == -1 && data[chan].getMaterial().equals(SevenADCChannelsManager.Material.WATER))
        maxWaterLevel = chan;
      if (ansiConsole)
        AnsiConsole.out.println(str);
    }
    if (ansiConsole)
    {
      str =        "+---+--------+---------+";
      AnsiConsole.out.println(str);    
    }
    int oilThickness = Math.max(0, maxOilLevel - maxWaterLevel); // (maxOilLevel == -1 ? 0 : (maxOilLevel - (maxWaterLevel == -1 ? 0 : maxWaterLevel)));
    if (ansiConsole)
    {
      str =        "WL:" + maxWaterLevel + ", OL:" + maxOilLevel + ", OT:" + oilThickness + "      ";
      AnsiConsole.out.println(str);    
    }
    if (webSocketClient != null)
    {
      JSONObject json = new JSONObject();
      json.put("water", maxWaterLevel + 1);
      json.put("oil", maxOilLevel + 1);
      try { webSocketClient.send(json.toString()); } // [1..100]
      catch (Exception ex) 
      { 
        displayAppErr(ex);
    //  ex.printStackTrace(); 
      }
    }
//  log(">>> To BusinessLogic (" + maxWaterLevel + ", " + maxOilLevel + ")");
    businessLogic(maxWaterLevel, maxOilLevel);
  }
  
  private static void businessLogic(int waterLevel, int oilLevel)
  {
//  log(">>> In BusinessLogic (" + waterLevel + ", " + oilLevel + ")");
    int oilThickness = Math.max(0, oilLevel - waterLevel);
    currentWaterLevel   = waterLevel;
    currentOilThickness = oilThickness;
    
//  System.out.println("Business Logic - Water:" + waterLevel + ", oil:" + oilLevel);
    if (oilLevel > -1)
    {
//    log("Oil thick:" + oilThickness + ", Water:" + waterLevel);
      if (waterLevel < 0 && oilThickness > 0)
      {
        // Switch the relay off?
        RelayManager.RelayState status = rm.getStatus("00");
     // log("Relay is:" + status);
        if (RelayManager.RelayState.ON.equals(status))
        {
          log("Turning relay off!");
          try { rm.set("00", RelayManager.RelayState.OFF); }
          catch (Exception ex)
          {
            System.err.println(ex.toString());
          }
        }        
        if (currentStatus.equals(ProcessStatus.ALL_OK))
        {
          log("Oil thick:" + oilThickness + ", Water:" + waterLevel + " (Oil Level:" + oilLevel + ")");
          // Make a call
          String[] mess = {"Oil the bilge of " + boatName + ": " + oilThickness + ".",
                            "Please reply CLEAN to this message when done with it."};
      //  String mess = "First warning to " + boatName;
          
          displayAppMess(" >>>>>>>>>> CALLING " + phoneNumber_1); // + "Mess is a " + mess.getClass().getName() + "\n" + mess);
          sendSMS(phoneNumber_1, mess);
          currentStatus = ProcessStatus.MESSAGE_SENT_TO_CAPTAIN;
          WaitForCleanThread wfct = new WaitForCleanThread();
          wfct.start();
        }
      }
      else
      {
        System.out.println("                            ");
        System.out.println("                            ");
      }
    }    
  }
  
  @Override
  public void setTypeOfChannel(int channel, SevenADCChannelsManager.Material material, float val)
  {
    data[channel] = new LevelMaterial(val, material);
    manageData();
    // Debug
    if (ansiConsole)
    {
      AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 20 + channel));
      Date now = new Date();
      AnsiConsole.out.println(now.toString() + ": Channel " + channel + " >> (" + DF31.format(val) + ") " + materialToString(material) + "       ");
    }
  }

  @Override
  public void genericSuccess(String string)
  {
    log(string);
  }
  
  @Override
  public void sendSuccess(String string)
  {
    if (sendMessWaiter != null)
    {
      synchronized (sendMessWaiter)
      {
        sendMessWaiter.notify();
        log("Released waiter...");
      }
    }
  }

  @Override
  public void genericFailure(String string)
  {
    log(string);
  }

  @Override
  public void adcState(String string)
  {
    log(string);
  }

  @Override
  public void batteryState(String string)
  {
    log(string);
  }

  @Override
  public void ccidState(String string)
  {
    log(string);
  }

  @Override
  public void rssiState(String string)
  {
    log(string);
  }

  @Override
  public void networkState(String string)
  {
    log(string);
  }

  @Override
  public void numberOfMessages(int i)
  {
    log("Nb mess:" + i);
  }

  @Override
  public void message(ReadWriteFONA.SMS sms)
  {
    log("\nReceived messsage:");
    log("From:" + sms.getFrom());
    log(sms.getContent());
    if (sms.getContent().equalsIgnoreCase("CLEAN") && (SIMULATOR.equals(sms.getFrom()) ||
                                                       sms.getFrom().contains(phoneNumber_1) ||
                                                       sms.getFrom().contains(phoneNumber_2) ||
                                                       sms.getFrom().contains(phoneNumber_3)))
    {
      // Check, and Resume
      if (currentOilThickness <= 0)
      {
        // Tell whoever has been warned so far
        boolean[] messLevel = { false, false, false };
        messLevel[SENT_TO_CAPTAIN] = (currentStatus == ProcessStatus.MESSAGE_SENT_TO_CAPTAIN ||
                                      currentStatus == ProcessStatus.MESSAGE_SENT_TO_OWNER ||
                                      currentStatus == ProcessStatus.MESSAGE_SENT_TO_AUTHORITIES);
        messLevel[SENT_TO_OWNER] = (currentStatus == ProcessStatus.MESSAGE_SENT_TO_OWNER ||
                                    currentStatus == ProcessStatus.MESSAGE_SENT_TO_AUTHORITIES);
        messLevel[SENT_TO_AUTHORITIES] = (currentStatus == ProcessStatus.MESSAGE_SENT_TO_AUTHORITIES);
        String[] mess = {"Oil in the bilge of " + boatName + " has been cleaned",
                         "Bilge pump power has been restored." };
    //  String mess =  "Oil in the bilge of " + boatName + " has been cleaned.";
        if (messLevel[SENT_TO_AUTHORITIES])
          sendSMS(phoneNumber_3, mess);
        if (messLevel[SENT_TO_OWNER])
          sendSMS(phoneNumber_2, mess);
        if (messLevel[SENT_TO_CAPTAIN])
          sendSMS(phoneNumber_1, mess);

        currentStatus = ProcessStatus.ALL_OK;
        RelayManager.RelayState status = rm.getStatus("00");
     // log("Relay is:" + status);
        if (RelayManager.RelayState.OFF.equals(status))
        {
          log("Turning relay back on.");
          try { rm.set("00", RelayManager.RelayState.ON); }
          catch (Exception ex)
          {
            System.err.println(ex.toString());
          }
        }
      } 
      else
      {
        // Reply, not clean enough.
        String[] mess = {"Sorry, the bilge is not clean enough. Power NOT restored",
                         "Try again to send a CLEAN message when this has been taken care of"};
     // String mess = "Not clean enough";
        sendSMS(phoneNumber_1, mess);
      }
    }
  }

  @Override
  public void ready()
  {
    log("FONA Ready!");
    fonaReady = true;
  }
  
  public final static void displayAppMess(String mess)
  {
    if (false && ansiConsole)
    {
      AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 50));
      AnsiConsole.out.println(rpad(mess, " ", 80));    
    }
    else
      log(mess);
  }

  public final static void displayAppErr(Exception ex)
  {
    if (ansiConsole)
    {
      AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 60));
      AnsiConsole.out.println(rpad(ex.toString(), " ", 80));    
    }
    else 
      ex.printStackTrace();
  }

  private abstract static class Tuple<X, Y>
  {
    public final X x;
    public final Y y;

    public Tuple(X x, Y y)
    {
      this.x = x;
      this.y = y;
    }

    @Override
    public String toString()
    {
      return "(" + x + "," + y + ")";
    }

    @Override
    public boolean equals(Object other)
    {
      if (other == null)
      {
        return false;
      }
      if (other == this)
      {
        return true;
      }
      if (!(other instanceof Tuple))
      {
        return false;
      }
      Tuple<X, Y> other_ = (Tuple<X, Y>) other;
      return other_.x == this.x && other_.y == this.y;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((x == null)? 0: x.hashCode());
      result = prime * result + ((y == null)? 0: y.hashCode());
      return result;
    }
  }
  
  public static class LevelMaterial<X, Y> extends Tuple<X, Y>
  {
    public LevelMaterial(X x, Y y)
    {
      super(x, y);    
    }
    
    public X getPercent() { return this.x; }
    public Y getMaterial() { return this.y; }
  }
  
  public static Properties getAppProperties()
  {
    return props;  
  }
  
  public static void log(String s)
  {
    if (fileLogger != null)
    {
      try
      {
        fileLogger.write(s + "\n");;
        fileLogger.flush();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    else
      System.out.println(s);
  }
  
  public static void main(String[] args) throws Exception
  {
    System.out.println(args.length + " parameter(s).");    
    props = new Properties();
    try 
    {
      props.load(new FileInputStream("props.properties"));
    }
    catch (IOException ioe)
    {
      displayAppErr(ioe);
  //  ioe.printStackTrace();
    }
    try 
    { 
      cleaningDelay = Long.parseLong(props.getProperty("cleaning.delay", "86400")); // Default: one day
    }
    catch (NumberFormatException nfe)
    {
      nfe.printStackTrace();
    }
    
    try
    {
      fileLogger = new BufferedWriter(new FileWriter(LOG_FILE));
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    LelandPrototype lp = new LelandPrototype();
    final ReadWriteFONA fona;
    
    if ("true".equals(props.getProperty("with.fona", "false")))
    {
      System.setProperty("baud.rate",   props.getProperty("baud.rate"));
      System.setProperty("serial.port", props.getProperty("serial.port"));
      
      fona = new ReadWriteFONA(lp);
      fona.openSerialInput();
      fona.startListening();
      while (!fonaReady)
      {
        System.out.println("Waiting for the FONA device to come up...");
        try { Thread.sleep(1000L); } catch (InterruptedException ie) {}
      }
      displayAppMess(">>> FONA Ready, moving on");
      smsProvider = fona;
    }
    else
    {
      System.out.println("Will simulate the phone calls.");
    }
    delay(1);
    
    final SevenADCChannelsManager sacm = new SevenADCChannelsManager(lp);

    wsUri         = props.getProperty("ws.uri", "ws://localhost:9876/"); 
    phoneNumber_1 = props.getProperty("phone.number.1", "14153505547");
    phoneNumber_2 = props.getProperty("phone.number.2", "14153505547");
    phoneNumber_3 = props.getProperty("phone.number.3", "14153505547");
    boatName      = props.getProperty("boat.name", "Never Again XXIII");
    
    try 
    { 
      rm = new RelayManager(); 
      rm.set("00", RelayManager.RelayState.ON);
    }
    catch (Exception ex)
    {
      System.err.println("You're not on the PI, hey?");
      ex.printStackTrace();
    }    
    
    initWebSocketConnection(wsUri);
    
    // CLS
    if (ansiConsole)
      AnsiConsole.out.println(EscapeSeq.ANSI_CLS); 

    final Thread me = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread()
       {
         public void run()
         {
           // Cleanup
           System.out.println();
           sacm.quit();
           if (smsProvider != null)
             smsProvider.closeChannel();
           synchronized (me)
           {
             me.notify();
           }
           gpio.shutdown();
           System.out.println("Program stopped by user's request.");
         }
       });
    synchronized (me)
    {
      System.out.println("Main thread waiting...");
      me.wait();
    }
    System.out.println("Done.");
  }
  
  private static void delay(float sec)
  {
    try { Thread.sleep(Math.round(1000 * sec)); } catch (InterruptedException ie) {}
  }
  
  public static class WaitForCleanThread extends Thread
  {
    private boolean keepWaiting = true;
    private long started = 0L;
      
    public void stopWaiting()
    {
      this.keepWaiting = false;
    }
    
    public void run()
    {      
      started = System.currentTimeMillis();
      while (keepWaiting && !currentStatus.equals(ProcessStatus.ALL_OK))
      {
        delay(10); // in seconds
        if (!currentStatus.equals(ProcessStatus.ALL_OK) && 
            (System.currentTimeMillis() - started) > (cleaningDelay * 1000)) // Expired
        {
          // Next status level.
          log("Your cleaning delay (" + cleaningDelay + ") has expired. Going to the next level");
          log(" >>>>>>>>>> Level is " + currentStatus + ", GOING TO THE NEXT LEVEL >>>>>> ");
          switch (currentStatus.level())
          {
            case SENT_TO_CAPTAIN:
              {
                log(">>>>>>>>>>>>> SENDING MESSAGE TO OWNER >>>>>");
                started = System.currentTimeMillis(); // Re-initialize the loop
                currentStatus = ProcessStatus.MESSAGE_SENT_TO_OWNER;
                String[] mess = {"Your boat, " + boatName + ", has oil in its bilge",
                                 "The power supply of the bilge pump has been shut off",
                                 "This oil should be cleaned before the power is restored",
                                 "Reply to this message by sending CLEAN to restore the power when done"};
            //  String mess = "Your boat, " + boatName + ", has oil in its bilge.";
                sendSMS(phoneNumber_2, mess);
              }
              break;
            case SENT_TO_OWNER:
              {
                log(">>>>>>>>>>>>> SENDING MESSAGE TO AUTHORITIES >>>>>");
                started = System.currentTimeMillis(); // Re-initialize the loop
                currentStatus = ProcessStatus.MESSAGE_SENT_TO_AUTHORITIES;
                String[] mess = {"The vessel " + boatName + " has oil in its bilge",
                                 "The power supply of the bilge pump has been shut off",
                                 "This oil should be cleaned before the power is restored",
                                 "Reply to this message by sending CLEAN to allow the power to be restored"};
            //  String mess = "The vessel " + boatName + " has oil in its bilge."};
                sendSMS(phoneNumber_3, mess);
              }
              break;
            default:
              log(">>>>>>>>>>>>> FULL RESET NEEDED >>>>>");
              keepWaiting = false; // Full reset needed.
              break;
          }
        }
      }
      log("  >>> " + this.getClass().getName() + " completed.");
    }
  }
}
