package fona.arduino;


import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPortException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Write data, from the Raspberry to the Arduino, through the serial port.
 * Receives a response from the Arduino.
 * 
 *  Commands are:

  a   read the ADC 2.8V max (FONA800 & 808)
  b   read the Battery V and % charged
  C   read the SIM CCID
  i   read RSSI
  n   read network status

  N   Number of SMSs
  r|x Read SMS # x
  d|x Delete SMS # x
  s|<dest number>|<mess payload> Send SMS  to <dest number>
  
  Message received: +CMTI: "SM",3 <- where 3 is the number of the message just received.

   TODO The list of the responses from the Arduino, with their meaning.
 */ 
public class ReadWrite
{
  private static boolean verbose = true;
  private static boolean getVerbose()
  {
    return verbose;
  }

  private static Method GENERIC_FAILURE_PARSER;
  private static Method GENERIC_SUCCESS_PARSER;
  private static Method INCOMING_MESSAGE_MANAGER;
  static {
    try { GENERIC_FAILURE_PARSER = ReadWrite.class.getMethod("genericFailureParser", String.class); } catch (Exception ex) { ex.printStackTrace(); }
    try { GENERIC_SUCCESS_PARSER = ReadWrite.class.getMethod("genericSuccessParser", String.class); } catch (Exception ex) { ex.printStackTrace(); }
    try { INCOMING_MESSAGE_MANAGER = ReadWrite.class.getMethod("incomingMessageManager", String.class); } catch (Exception ex) { ex.printStackTrace(); }
  }

  public enum ArduinoMessagePrefix
  {
    FONA_OK       (">> FONA READY", "Good to go",          GENERIC_SUCCESS_PARSER),
    INCOMING_MESS ("+CMTI:",        "Incoming message",    INCOMING_MESSAGE_MANAGER),
    BAT_OK        (">> BAT OK",     "Read Battery",        GENERIC_SUCCESS_PARSER),
    BAT_FAILED    (">> BAT FAILED", "Read Battery failed", GENERIC_FAILURE_PARSER);

    private final String prefix;
    private final String meaning;
    private final Method parser;
    ArduinoMessagePrefix(String prefix, String meaning, Method parser)
    {
      this.prefix = prefix;
      this.meaning = meaning;
      this.parser = parser;
    }

    public String prefix()  { return this.prefix; }
    public String meaning() { return this.meaning; }
    public Method parser()  { return this.parser; }
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
    final Serial serial = SerialFactory.createInstance();

    // create and register the serial data listener
    serial.addListener(new SerialDataListener()
    {
      @Override
      public void dataReceived(SerialDataEvent event)
      {
        // print out the data received to the console
        String payload = event.getData();
        if (getVerbose())
        {
          System.out.print("Arduino said:" + payload);
        }
        // Manage data here. Check in the enum ArduinoMessagePrefix
        try
        {
          takeAction(payload);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });

    try
    {
      System.out.println("Hit 'Q' to quit.");
      System.out.println("Hit 'V' to toggle verbose on/off.");
      System.out.println("Hit [return] when ready to start.");
      userInput("");

      System.out.println("Opening port [" + port + ":" + Integer.toString(br) + "]");
      serial.open(port, br);
      System.out.println("Port is opened.");

      final Thread me = Thread.currentThread();
      Thread userInputThread = new Thread()
        {
          public void run()
          {
            boolean loop = true;
            while (loop)
            {
              String userInput = "";
              userInput = userInput("So? > ");
              if ("Q".equalsIgnoreCase(userInput))
                loop = false;
              else if ("V".equalsIgnoreCase(userInput))
                verbose = !verbose;
              else
              { 
                if (serial.isOpen())
                {
                  System.out.println("\tWriting [" + userInput + "] to the serial port...");
                  try
                  {
                    serial.write(userInput + "\n");
                  }
                  catch (IllegalStateException ex)
                  {
                    ex.printStackTrace();
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
      serial.close();
    }
    catch (SerialPortException ex)
    {
      System.out.println(" ==>> SERIAL SETUP FAILED : " + ex.getMessage());
      return;
    }
    System.exit(0);
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

  private static void takeAction(String mess) throws Exception
  {
    ArduinoMessagePrefix amp = findCommand(mess);
    if (amp != null)
    {
      String meaning = amp.meaning();
      Method parser = amp.parser();
      if (parser != null)
      {
        parser.invoke(ReadWrite.class, mess);
      }
    }
    else
      System.out.println("Command [" + mess + "] unknown.");
  }

  private static ArduinoMessagePrefix findCommand(String message)
  {
    ArduinoMessagePrefix ret = null;
    for (ArduinoMessagePrefix amp : ArduinoMessagePrefix.values())
    {
      if (message.startsWith(amp.prefix()))
      {
        ret = amp;
        break;
      }
    }
    return ret;
  }

  public static void genericSuccessParser(String message)
  {
    System.out.println("Generic success:" + message);
  }
  public static void genericFailureParser(String message)
  {
    System.out.println("Generic failure:" + message);
  }
  public static void incomingMessageManager(String message)
  {
    // +CMTI: "SM",3
    System.out.println("Incoming message:" + message);
    String[] sa = message.split(",");
    if (sa.length == 2) {
      // Build the command that will read the new message:
      String readMessCmd = "r|" + sa[1].trim();
      // TODO Send the command to the serial port

    }
  }
}
