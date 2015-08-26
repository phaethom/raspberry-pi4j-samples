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

  private static ReadWrite instance = this;

  public enum ArduinoMessagePrefix
  {
    FONA_OK       (">> FONA READY", "Good to go", null),
    INCOMING_MESS ("+CMTI:",        "Incoming message", null),
    BAT_OK        (">> BAT OK",     "Read Battery", null),
    BAT_FAILED    (">> BAT FAILED", "Read Battery failed", instance.getClass().getMethod("genericFailureParser", String.class));

    private final String prefix;
    private final String meaning;
    private Method parser;
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
        ArduinoMessagePrefix amp = findCommand(payload);
        if (amp != null)
        {
          String meaning = amp.meaning();
          Method parser = amp.parser();
          if (parser != null)
          {
            parser.invoke(instance, payload);
          }
        }
        else
          System.out.println("Command [" + payload + "] unknown.");
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

  private static void genericFailureParser(String message)
  {

  }
}
