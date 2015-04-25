package arduino.raspberrypi;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPortException;

public class SeriaLevellReader
{
  // NMEA Style
  public static int calculateCheckSum(String str)
  {
    int cs = 0;
    char[] ca = str.toCharArray();
    for (int i=0; i<ca.length; i++)
    {
      cs ^= ca[i]; // XOR
//    System.out.println("\tCS[" + i + "] (" + ca[i] + "):" + Integer.toHexString(cs));
    }
    return cs;
  }
  
  // NMEA Style
  public static boolean validCheckSum(String data, boolean verb)
  {
    String sentence = data.trim();
    boolean b = false;    
    try
    {
      int starIndex = sentence.indexOf("*");
      if (starIndex < 0)
        return false;
      String csKey = sentence.substring(starIndex + 1);
      int csk = Integer.parseInt(csKey, 16);
      String str2validate = sentence.substring(1, sentence.indexOf("*"));
      int calcCheckSum = calculateCheckSum(str2validate);
      b = (calcCheckSum == csk);
    }
    catch (Exception ex)
    {
      if (verb) System.err.println("Oops:" + ex.getMessage());
    }
    return b;
  }

  /*
   * Sample payload:
$OSMSG,LEVEL,4,1021*09
$OSMSG,LEVEL,0,0*3F
$OSMSG,LEVEL,5,1023*0A
$OSMSG,LEVEL,0,0*3F
$OSMSG,LEVEL,5,1022*0B
$OSMSG,LEVEL,3,1020*0F
$OSMSG,LEVEL,4,1018*03
$OSMSG,LEVEL,3,1019*05
$OSMSG,LEVEL,2,1019*04
$OSMSG,LEVEL,1,1020*0D
$OSMSG,LEVEL,5,1021*08
$OSMSG,LEVEL,1,1021*0C
$OSMSG,LEVEL,0,0*3F
$OSMSG,LEVEL,5,1023*0A
$OSMSG,LEVEL,0,0*3F
$OSMSG,LEVEL,4,1020*08
$OSMSG,LEVEL,0,0*3F

The message is assumed to be valid
   */
  private static int parseMessage(String message)
  {
    int level = -1;
    String[] data = message.split(",");
    try 
    {
      level = Integer.parseInt(data[2]);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return level;
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
    System.out.println(" ... connect on " + port + " using settings: " + Integer.toString(br) +  ", N, 8, 1.");
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
        if (validCheckSum(payload, false))
        {
          int level = parseMessage(payload);
          System.out.print("Arduino said level is :" + level);
        }
        else
          System.out.println("\tOops! Invalid String [" + payload + "]");
      }
    });

    try
    {
      // open the default serial port provided on the GPIO header
      System.out.println("Opening port [" + port + ":" + Integer.toString(br) + "]");
      serial.open(port, br);
      System.out.println("Port is opened.");

      Thread me = Thread.currentThread();
      synchronized (me)
      {
        me.wait();
      }
    }
    catch (SerialPortException ex)
    {
      System.out.println(" ==>> Serial Setup Failed : " + ex.getMessage());
      return;
    }
  }
}
