package adc.levelreader.main;

import adc.utils.EscapeSeq;

import fona.arduino.ReadWriteFONA;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

import org.fusesource.jansi.AnsiConsole;

import adc.levelreader.manager.AirWaterOilInterface;
import adc.levelreader.manager.SevenADCChannelsManager;

import fona.arduino.FONAClient;

import java.io.FileInputStream;

import java.io.IOException;

import java.net.URI;

import java.util.Properties;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

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
public class LelandPrototype implements AirWaterOilInterface, FONAClient
{
  private static long CLEANING_DELAY = 0L;
  static
  {
    try 
    { 
      CLEANING_DELAY = Long.parseLong(System.getProperty("cleaning.delay", "86400")); // Default: one day
    }
    catch (NumberFormatException nfe)
    {
      nfe.printStackTrace();
    }
  }
  
  private static LevelMaterial<Float, SevenADCChannelsManager.Material>[] data = null;
  private final static NumberFormat DF31 = new DecimalFormat("000.0");
  private final static NumberFormat DF4  = new DecimalFormat("###0");
  private static WebSocketClient webSocketClient = null;
  private static ReadWriteFONA smsProvider = null;
  private static RelayManager rm = null;
  
  private static String wsUri = "";
  private static String phoneNumber_1 = "", 
                        phoneNumber_2 = "",
                        phoneNumber_3 = "";
  private static String boatName = "";
  
  private static boolean fonaReady = false;
  
  public enum ProcessStatus
  {
    ALL_OK,
    MESSAGE_SENT_TO_CAPTAIN,
    MESSAGE_SENT_TO_OWNER,
    MESSAGE_SENT_TO_AUTHORITIES
  }
  
  private final static int SENT_TO_CAPTAIN     = 0;
  private final static int SENT_TO_OWNER       = 1;
  private final static int SENT_TO_AUTHORITIES = 2;
  
  private static ProcessStatus currentStatus = ProcessStatus.ALL_OK;
  
  private static int currentWaterLevel   = 0;
  private static int currentOilThickness = 0;
  
  public LelandPrototype()
  {
    data = new LevelMaterial[7];
    for (int i=0; i<data.length; i++)
    {
      data[i] = new LevelMaterial<Float, SevenADCChannelsManager.Material>(0f, SevenADCChannelsManager.Material.UNKNOWN);
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
          System.out.println("WS On Open");
        }

        @Override
        public void onMessage(String string)
        {
  //        System.out.println("WS On Message");
        }

        @Override
        public void onClose(int i, String string, boolean b)
        {
          System.out.println("WS On Close");
        }

        @Override
        public void onError(Exception exception)
        {
          System.out.println("WS On Error");
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

  private static void sendSMS(String to,
                              String content)
  {
    if (smsProvider != null)
    {
      String mess = content;
      if (mess.length() > 140)
        mess = mess.substring(0, 140);
      smsProvider.sendMess(to, mess);
    }
    else
      System.out.println(">>> Simulating call to " + to + ", " + content);
  }
  // User Interface ... Sovietic! And business logic.
  private static void manageData()
  {
    int maxWaterLevel = -1;
    int maxOilLevel   = -1;
    // Clear the screen, cursor on top left.
 // AnsiConsole.out.println(EscapeSeq.ANSI_CLS); 
    AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 1));
    String str = "+---+--------+---------+";
    AnsiConsole.out.println(str);
    str =        "| C |  Vol % |   Mat   |";
    AnsiConsole.out.println(str);
    
    str =        "+---+--------+---------+";
    AnsiConsole.out.println(str);
    for (int chan=data.length - 1; chan >= 0; chan--) // Top to bottom
    {
      str = "| " + Integer.toString(chan) + " | " +
                   lpad(DF4.format(data[chan].getPercent()), " ", 4) + " % | " +
                   lpad(materialToString(data[chan].getMaterial()), " ", 7) + " |";
      if (maxOilLevel == -1 && data[chan].getMaterial().equals(SevenADCChannelsManager.Material.OIL))
        maxOilLevel = chan;
      if (maxWaterLevel == -1 && data[chan].getMaterial().equals(SevenADCChannelsManager.Material.WATER))
        maxWaterLevel = chan;
      AnsiConsole.out.println(str);
    }
    str =        "+---+--------+---------+";
    AnsiConsole.out.println(str);    
    
    int volume = (int)(100 * ((maxWaterLevel==-1?0:maxWaterLevel) / 7d));
    try { webSocketClient.send(Integer.toString(volume)); } // [1..100]
    catch (Exception ex) 
    { 
      displayAppErr(ex);
  //  ex.printStackTrace(); 
    }
    businessLogic(maxWaterLevel, maxOilLevel);
  }
  
  private static void businessLogic(int waterLevel, int oilLevel)
  {
    int oilThickness = oilLevel - waterLevel;
    currentWaterLevel   = waterLevel;
    currentOilThickness = oilThickness;
    
//  System.out.println("Max Water:" + waterLevel);
    if (oilLevel > -1)
    {
      System.out.println("Oil thick:" + oilThickness);
      if (waterLevel <= 0 && oilThickness > 0)
      {
        // Switch the relay off?
        RelayManager.RelayState status = rm.getStatus("00");
    //  System.out.println("Relay is:" + status);
        if (RelayManager.RelayState.ON.equals(status))
        {
          System.out.println("Turning relay off!");
          try { rm.set("00", RelayManager.RelayState.OFF); }
          catch (Exception ex)
          {
            System.err.println(ex.toString());
          }
        }        
        // Make a call
        String mess = "In the bilge of " + boatName + ", oil thickness is " + oilThickness + "\n" +
                      "Please clean this, and reply 'CLEAN' to this message when done.";
        
        displayAppMess(" >>>>>>>>>> CALLING !!!!! \n" + phoneNumber_1 + " :\n" + mess);
        sendSMS(phoneNumber_1, mess);
        currentStatus = ProcessStatus.MESSAGE_SENT_TO_CAPTAIN;
        WaitForCleanThread wfct = new WaitForCleanThread();
        wfct.start();
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
    AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 20 + channel));
    Date now = new Date();
    AnsiConsole.out.println(now.toString() + ": Channel " + channel + " >> (" + DF31.format(val) + ") " + materialToString(material) + "       ");
  }

  @Override
  public void genericSuccess(String string)
  {
    System.out.println(string);
  }

  @Override
  public void genericFailure(String string)
  {
    System.out.println(string);
  }

  @Override
  public void adcState(String string)
  {
    System.out.println(string);
  }

  @Override
  public void batteryState(String string)
  {
    System.out.println(string);
  }

  @Override
  public void ccidState(String string)
  {
    System.out.println(string);
  }

  @Override
  public void rssiState(String string)
  {
    System.out.println(string);
  }

  @Override
  public void networkState(String string)
  {
    System.out.println(string);
  }

  @Override
  public void numberOfMessages(int i)
  {
    System.out.println("Nb mess:" + i);
  }

  @Override
  public void message(ReadWriteFONA.SMS sms)
  {
    System.out.println("\nReceived messsage:");
    System.out.println("From:" + sms.getFrom());
    System.out.println(sms.getContent());
    if (sms.getContent().equalsIgnoreCase("CLEAN") && sms.getFrom().equals(phoneNumber_1))
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
        String mess = "Oil in the bilge of '" + boatName + "' has been cleaned.\n" +
                      "Bilge pump power has been restored.";
        if (messLevel[SENT_TO_AUTHORITIES])
          sendSMS(phoneNumber_3, mess);
        if (messLevel[SENT_TO_OWNER])
          sendSMS(phoneNumber_2, mess);
        if (messLevel[SENT_TO_CAPTAIN])
          sendSMS(phoneNumber_1, mess);

        currentStatus = ProcessStatus.ALL_OK;
        RelayManager.RelayState status = rm.getStatus("00");
        //  System.out.println("Relay is:" + status);
        if (RelayManager.RelayState.OFF.equals(status))
        {
          System.out.println("Turning relay back on.");
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
        String content = "Sorry, the bilge is not clean enough.\nPower NOT restored.\n" +
                         "Try again to send a 'CLEAN' message when this has been taken care of.";
        sendSMS(phoneNumber_1, content);
      }
    }
  }

  @Override
  public void ready()
  {
    System.out.println("FONA Ready!");
    fonaReady = true;
  }
  
  public final static void displayAppMess(String mess)
  {
    AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 40));
    AnsiConsole.out.println(rpad(mess, " ", 80));    
  }

  public final static void displayAppErr(Exception ex)
  {
    AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 50));
    AnsiConsole.out.println(rpad(ex.toString(), " ", 80));    
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
  
  public static void main(String[] args) throws Exception
  {
    System.out.println(args.length + " parameter(s).");    
    Properties props = new Properties();
    try 
    {
      props.load(new FileInputStream("props.properties"));
    }
    catch (IOException ioe)
    {
      displayAppErr(ioe);
  //  ioe.printStackTrace();
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
    final SevenADCChannelsManager sac = new SevenADCChannelsManager(lp);

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
    AnsiConsole.out.println(EscapeSeq.ANSI_CLS); 

    final Thread me = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread()
       {
         public void run()
         {
           // Cleanup
           System.out.println();
           sac.quit();
           if (smsProvider != null)
             smsProvider.closeChannel();
           synchronized (me)
           {
             me.notify();
           }
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
      while (keepWaiting && currentStatus != ProcessStatus.ALL_OK)
      {
        try { Thread.sleep(60 * 1000L); } catch (InterruptedException ie) {} // Wait 1 minute.
        if (System.currentTimeMillis() - started > CLEANING_DELAY) // Expired
        {
          // Next status level.
          System.out.println("Your cleaning delay has expired. Going to the next level");
          displayAppMess(" >>>>>>>>>> GOING TO THE NEXT LEVEL >>>>>> \n");
          if (currentStatus == ProcessStatus.MESSAGE_SENT_TO_CAPTAIN)
          {
            started = System.currentTimeMillis(); // Re-initialize the loop
            currentStatus = ProcessStatus.MESSAGE_SENT_TO_OWNER;
            String mess = "Your boat, '" + boatName + "', has oil in its bilge.\n" +
                          "The power supply of the bilge pump has been shut off.\n" +
                          "This oil should be cleaned before the power is restored.\n" +
                          "You can reply to this message by sending 'CLEAN' to restore the power when done.";
            sendSMS(phoneNumber_2, mess);
          }
          else if (currentStatus == ProcessStatus.MESSAGE_SENT_TO_OWNER)
          {
            started = System.currentTimeMillis(); // Re-initialize the loop
            currentStatus = ProcessStatus.MESSAGE_SENT_TO_AUTHORITIES;
            String mess = "The vessel '" + boatName + "' has oil in its bilge.\n" +
                          "The power supply of the bilge pump has been shut off.\n" +
                          "This oil should be cleaned before the power is restored.\n" +
                          "You can reply to this message by sending 'CLEAN' to allow the power to be restored.";
            sendSMS(phoneNumber_3, mess);
          }
          else
            keepWaiting = false; // Full reset needed.
        }
      }
    }
  }
}
