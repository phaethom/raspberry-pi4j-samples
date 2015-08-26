package fona.send;


import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPortException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Important: Makes sure you've run
 * Prompt> rpi-serial-console disable
 * and re-booted.
 */ 
public class SendSMS
{
  private final static Serial serial = SerialFactory.createInstance();
  
  private static String expectedNotification = "";
  private static Thread expectingNotification = null;
  
  private final static int CLTR_Z = 0x1A; // 26
  
  private static boolean verbose = true;
  private static boolean getVerbose()
  {
    return verbose;
  }
  
  private static boolean readSerial = true;
  private static boolean keepReading() { return readSerial; }
  private static void stopReading() { readSerial = false; }
  
  private static boolean connectionEstablished = false;
  
  private static byte[] readSerial()
  {
    byte[] ba = null;
    int ab = serial.availableBytes();
    
    while (ab > 0)
    {
      try
      {
        char c = serial.read();
        System.out.println("    >> Read char 0x" + Integer.toHexString((int)c).toUpperCase() + " (" + ((int)c) + ")");
        ab = serial.availableBytes();
      }
      catch (IllegalStateException ise)
      {
        // Not opened yet
      }
      catch (Exception ex)
      {
        System.err.println(ex.toString());            
      }
    }
    return ba;
  }
  
  public static void main(String args[])
    throws InterruptedException, NumberFormatException
  {
    String port = System.getProperty("serial.port", Serial.DEFAULT_COM_PORT);
    int br = Integer.parseInt(System.getProperty("baud.rate", "9600"));
    if (args.length > 0)
    {
      try
      {
        br = Integer.parseInt(args[0]);
      }
      catch (Exception ex)
      {
        System.err.println(ex.getMessage());
      }
    }
    
    System.out.println("Serial Communication.");
    System.out.println(" ... connect using settings: " + Integer.toString(br) +  ", N, 8, 1.");
    System.out.println(" ... data received on serial port should be displayed below.");

    // create an instance of the serial communications class
 // final Serial serial = SerialFactory.createInstance();

    // Option: serial.read() in its own thread...
    Thread serialReader = new Thread()
    {
      private StringBuffer fullMessage = new StringBuffer();
      private final String ACK = "\r\nOK\r\n";  // QUESTION specially when entering a message.

      public void run()
      {
        while (keepReading())
        {
          try
          {
            int ab = serial.availableBytes();
            if (ab > 0)
            {
              char c = serial.read();
              System.out.println("    >> Read char 0x" + Integer.toHexString((int)c).toUpperCase() + " (" + ((int)c) + ")" + ((c >= 32 && c <= 127) ? ", [" + c + "]" : ""));

              fullMessage.append(c);
              if (fullMessage.toString().endsWith(ACK) || fullMessage.toString().startsWith(MESSAGE_PROMPT))
              {
              //        System.out.println("Flushing...");
                String mess = fullMessage.toString(); // Send the full message. Parsed later.
              //  mess = mess.substring(0, mess.length() - ACK.length() - 1);
                fonaOutput(mess);
                fullMessage = new StringBuffer();
              } 
            }
          }
          catch (IllegalStateException ise)
          {
            // Not opened yet
          }
          catch (Exception ex)
          {
            System.err.println(ex.toString());            
          }
        }
      }
    };
//  serialReader.start(); 
    
    // create and register the serial data listener
    final SerialDataListener sdl = new SerialDataListener()
    {
      private StringBuffer fullMessage = new StringBuffer();
      private final String ACK = "\r\nOK\r\n";  // QUESTION specially when entering a message.
      
      @Override
      public void dataReceived(SerialDataEvent event)
      {
        // print out the data received to the console
        String payload = event.getData();

        if (getVerbose())
        {
          for (int i=0; i<payload.length(); i++) 
          {
            System.out.print("0x" + Integer.toHexString(payload.charAt(i)).toUpperCase() + " ");
          }
          System.out.println();
          
          String trimmed = payload.trim();
          if (trimmed.length() > 0)
            System.out.print("\n   >> FONA said:" + trimmed + "\n");
        }

        fullMessage.append(payload);
        if (fullMessage.toString().endsWith(ACK) || fullMessage.toString().startsWith(MESSAGE_PROMPT))
        {
//        System.out.println("Flushing...");
          String mess = fullMessage.toString(); // Send the full message. Parsed later.
      //  mess = mess.substring(0, mess.length() - ACK.length() - 1);
          fonaOutput(mess);
          fullMessage = new StringBuffer();
        } 
      }
    };
    serial.addListener(sdl);
    
    try
    {
      System.out.println("Hit 'Q' to quit.");
      System.out.println("Hit 'V' to toggle verbose on/off.");
      userInput("Hit [return] when ready to start.");
    //userInput("");

      System.out.println("Opening port [" + port + ":" + Integer.toString(br) + "]");
      serial.open(port, br);
      System.out.println("Port is opened.");

      final Thread me = Thread.currentThread();
      Thread userInputThread = new Thread()
        {
          public void run()
          {
            System.out.println("Establishing connection (can take up to 3 seconds).");
            while (!connectionEstablished)
            {
              tryToConnect();
              if (!connectionEstablished)
              {
                delay(1);
              }
            }
            System.out.println("Connection established.");
            displayMenu();
            boolean loop = true;
            while (loop)
            {
              String userInput = "";
              userInput = userInput("FONA> ");
              if ("Q".equalsIgnoreCase(userInput))
                loop = false;
              else if ("V".equalsIgnoreCase(userInput))
                verbose = !verbose;
              else if ("?".equalsIgnoreCase(userInput))
                displayMenu();
              else
              { 
                if (serial.isOpen())
                {
                  String cmd = "";
                  if ("M".equals(userInput))
                    sendToFona("ATI");
                  else if ("D".equals(userInput))
                    sendToFona("AT+CMEE=2");
                  else if ("C".equals(userInput))
                    sendToFona("AT+CCID");
                  else if ("b".equals(userInput))
                    sendToFona("AT+CBC");
                  else if ("n".equals(userInput))
                    sendToFona("AT+COPS?");
                  else if ("I".equals(userInput))
                    sendToFona("AT+CREG");
                  else if ("i".equals(userInput))
                    sendToFona("AT+CSQ");
                  else if ("N".equals(userInput))
                  {
                    // Wait (notification) then send AT+CPMS?
                    expectingNotification = Thread.currentThread();
                    expectedNotification = "AT+CMGF=1";
                    sendToFona("AT+CMGF=1");
                    synchronized (expectingNotification)
                    {
                      try 
                      {
                        System.out.println("     ... Waiting");
                        expectingNotification.wait(); // TODO Timeout?
                        System.out.println("Moving on!");
                      }
                      catch (InterruptedException ie) {}
                    }
                    expectingNotification = null;
                    delay(1);
                    sendToFona("AT+CPMS?");
                  }
                  else if ("s".equals(userInput)) // Send SMS
                  {
                    // Wait (notification) then send AT+CPMS?
                    expectingNotification = Thread.currentThread();
                    expectedNotification = "AT+CMGF=1";
                    sendToFona("AT+CMGF=1");
                    synchronized (expectingNotification)
                    {
                      try 
                      {
                        System.out.println("     ... Waiting");
                        expectingNotification.wait(); // TODO Timeout?
                        System.out.println("Moving on!");
                      }
                      catch (InterruptedException ie) {}
                    }
                    expectingNotification = null;
                    String sendTo = "14153505547";
                    delay(1);
                    expectingNotification = Thread.currentThread();
                    expectedNotification = "AT+CMGS=\"";
                    sendToFona("AT+CMGS=\"" + sendTo + "\"");
                    synchronized (expectingNotification)
                    {
                      try 
                      {
                        System.out.println("     ... Waiting");
                        expectingNotification.wait(); // TODO Timeout?
                        System.out.println("Moving on, enter message");
                      }
                      catch (InterruptedException ie) {}
                    }
                    expectingNotification = null;
                    delay(1);
                    System.out.println("Message here...");
                    sendToFona("This is a message.\n\032");  // \032 = Ctrl^Z
                  }
                  else
                  {
                    cmd = userInput;
                    if (getVerbose())
                      System.out.println("\tWriting [" + cmd + "] to the serial port...");
                    try
                    {
                      serial.write(cmd + "\n");
                      serial.flush();
                    }
                    catch (IllegalStateException ex)
                    {
                      ex.printStackTrace();
                    }
                  }
                }
                else
                {
                  System.out.println("Not open yet...");
                }
              }
            }
            synchronized (me)
            {
              me.notify();
            }
          }
        };
      userInputThread.start();
      
      synchronized (me)
      {
        me.wait();
      }
      System.out.println("Bye!");
      stopReading();
      serial.close();
    }
    catch (SerialPortException ex)
    {
      System.out.println(" ==>> SERIAL SETUP FAILED : " + ex.getMessage());
      return;
    }
    System.exit(0);
  }

  private static void sendToFona(String payload)
  {
    if (serial.isOpen())
    {
      try
      {
        if (getVerbose())
          System.out.println("Writing to FONA: [" + payload + "]");
        serial.writeln(payload);
        /*
        if (!payload.endsWith("\032"))
          serial.write(payload + "\n");
        else
        {
          serial.write(payload.toCharArray());
          System.out.println("... Posting SMS.");
        }
        */
        serial.flush();
      }
      catch (IllegalStateException ex)
      {
        ex.printStackTrace();
      }
    }
  }

  private static void tryToConnect()
  {
    sendToFona("AT");
  }
  
  private static void displayMenu()
  {
    System.out.println("------------------------------");
    System.out.println("[?] Print this menu");
    System.out.println("[D] Turn DEBUG on");
    System.out.println("[M] Module name and revision");
    System.out.println("[b] Read the battery V");
    System.out.println("[C] Read the SIM CCID");
    System.out.println("[i] Read RSSI (signal strength)");
    System.out.println("[n] Get network name");
    System.out.println("[N] Number of SMSs");
    System.out.println("[r] Read SMS #");
    System.out.println("[R] Raed All SMSs");
    System.out.println("[d] Delete SMS #");
    System.out.println("[s] Send SMS");
    System.out.println("------------------------------");
  }
  
  private final static String CRLF = "\r\n";
  private final static String CRCRLF = "\r\r\n";
  private final static String CONNECTION_OK = "AT" + CRCRLF + "OK" + CRLF;
  private final static String ATI_RESPONSE  = "ATI" + CRCRLF;
  private final static String DEBUG_ON_RESPONSE  = "AT+CMEE=2" + CRCRLF;
  private final static String BATTERY_RESPONSE = "AT+CBC" + CRCRLF;
  private final static String SIGNAL_RESPONSE = "AT+CSQ" + CRCRLF;
  private final static String SIM_CARD_RESPONSE = "AT+CCID" + CRCRLF;
  private final static String NETWORK_NAME_RESPONSE = "AT+COPS?" + CRCRLF;
  private final static String SET_TO_TEXT_RESPONSE = "AT+CMGF=1" + CRCRLF;
  private final static String NUM_SMS_RESPONSE = "AT+CPMS?" + CRCRLF;
  private final static String MESSAGE_PROMPT = "AT+CMGS=\"";
  
  private static void fonaOutput(String mess)
  {
    if (getVerbose())
    {
      System.out.println("==== fonaOutput ====");
      for (int i=0; i<mess.length(); i++)
        System.out.print(" 0x" + Integer.toHexString(mess.charAt(i)).toUpperCase());
      System.out.println("\n" + mess);
      System.out.println("====================");
    }
    if (mess.equals(CONNECTION_OK))
    {
      connectionEstablished = true;
    }
    else if (mess.startsWith(ATI_RESPONSE))
    {
      int start = mess.indexOf(CRCRLF) + CRCRLF.length();
      int end   = mess.indexOf(CRLF, start + CRCRLF.length() + 1);
      String content = mess.substring(start, end);
      System.out.println("Module name and revision:" + content);
    }
    else if (mess.startsWith(DEBUG_ON_RESPONSE))
    {
      System.out.println("Debug is ON");
    }
    else if (mess.startsWith(BATTERY_RESPONSE))
    {
      int start = mess.indexOf(CRCRLF) + CRCRLF.length();
      int end   = mess.indexOf(CRLF, start + CRCRLF.length() + 1);
      String content = mess.substring(start, end);
      String[] parsed = content.substring("+CBC: ".length()).split(",");
      System.out.println("Load:" + parsed[1] + "%, " + parsed[2] + " mV");
    }
    else if (mess.startsWith(SIGNAL_RESPONSE))
    {
      int start = mess.indexOf(CRCRLF) + CRCRLF.length();
      int end   = mess.indexOf(CRLF, start + CRCRLF.length() + 1);
      String content = mess.substring(start, end);
      String[] parsed = content.substring("+CSQ: ".length()).split(",");
      System.out.println("Signal:" + parsed[0] + " dB. Must be higher than 5, the higher the better.");
    }
    else if (mess.startsWith(SIM_CARD_RESPONSE))
    {
      int start = mess.indexOf(CRCRLF) + CRCRLF.length();
      int end   = mess.indexOf(CRLF, start + CRCRLF.length() + 1);
      String content = mess.substring(start, end);
      System.out.println("SIM Card # " + content);
    }
    else if (mess.startsWith(NETWORK_NAME_RESPONSE))
    {
      int start = mess.indexOf(CRCRLF) + CRCRLF.length();
      int end   = mess.indexOf(CRLF, start + CRCRLF.length() + 1);
      String content = mess.substring(start, end);
      String[] parsed = content.substring("+COPS: ".length()).split(",");
      System.out.println("Network:" + parsed[2]);
    }
    else if (mess.startsWith(SET_TO_TEXT_RESPONSE))
    {
      // Release the waiting thread
      if (expectingNotification != null)
      {
        System.out.println("Releasing the waiter");
        synchronized (expectingNotification)
        {
          expectingNotification.notify();
        }
      }
      else
      {
        System.out.println("Weird: no one is waiting...");
      }
    }
    else if (mess.startsWith(NUM_SMS_RESPONSE))
    {   
      int start = mess.indexOf(CRCRLF) + CRCRLF.length();
      int end   = mess.indexOf(CRLF, start + CRCRLF.length() + 1);
      String content = mess.substring(start, end);
      String[] parsed = content.substring("+CPMS: ".length()).split(",");
      System.out.println("Number of SMS :" + parsed[1]);
    }
    else if (mess.startsWith(MESSAGE_PROMPT))
    {
      System.out.println("Enter message:" + mess);      
      // Release the waiting thread
      if (expectingNotification != null)
      {
        System.out.println("Releasing the waiter");
        synchronized (expectingNotification)
        {
          expectingNotification.notify();
        }
      }
      else
      {
        System.out.println("Weird: no one is waiting...");
      }
    }
    else
    {
      //Response is: <echo CMD>\n<Response>
      String[] sa = mess.split("\n");
      if (sa.length == 2)
        System.out.println("\n====== From FONA: ======\n" + 
                           sa[1] + 
                           "\n=========================");
      else
        System.out.println("                  Unknown format:[" + mess + "]");
    }
  }
  
  private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

  private static String userInput(String prompt)
  {
    String retString = "";
    System.err.print(prompt);
    try
    {
      retString = stdin.readLine();
    }
    catch(Exception e)
    {
      System.out.println(e);
      String s;
      try
      {
        s = userInput("<Oooch/>");
      }
      catch(Exception exception) 
      {
        exception.printStackTrace();
      }
    }
    return retString;
  }

  private static void delay(int delay)
  {
    try { Thread.sleep(delay * 1000L); } catch (InterruptedException ie) {}
  }
}
