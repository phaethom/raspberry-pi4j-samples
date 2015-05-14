package raspisamples.adc.levelreader.samples;

import adafruiti2c.sensor.AdafruitBMP180;

import adc.ADCObserver;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;

import java.text.SimpleDateFormat;

import java.util.Date;

import raspisamples.adc.levelreader.ADCChannels_1_to_8;

public class LevelAndTemperature
{
  private final static NumberFormat NF = new DecimalFormat("#0.00");
  private final static Format SDF = new SimpleDateFormat("hh:mm:ss");
  
  public static void main(String[] args) throws Exception
  {
    System.out.println(args.length + " parameter(s).");    
    /**
     * This is the list of the channels to listen to.
     */
    ADCObserver.MCP3008_input_channels[] listening2 = new ADCObserver.MCP3008_input_channels[] 
    {
      ADCObserver.MCP3008_input_channels.CH0,
      ADCObserver.MCP3008_input_channels.CH1,
      ADCObserver.MCP3008_input_channels.CH2,  
      ADCObserver.MCP3008_input_channels.CH3,  
      ADCObserver.MCP3008_input_channels.CH4, 
      ADCObserver.MCP3008_input_channels.CH5, 
      ADCObserver.MCP3008_input_channels.CH6 
    };
    
    final ADCChannels_1_to_8 sac = new ADCChannels_1_to_8(listening2);
    final AdafruitBMP180 bmp180  = new AdafruitBMP180();
    
    final Thread tempReader = new Thread()
      {
        public void run()
        {
          float temp  = 0;
          float originalTemp = 0;
          try { originalTemp = bmp180.readTemperature(); } 
          catch (Exception ex) 
          { 
            System.err.println(ex.getMessage()); 
            ex.printStackTrace();
          }
          System.out.println(">>> Original Temperature :" + NF.format(originalTemp) + "\272C");                          
          boolean go = true;
          while (go)
          {
            try 
            { 
              temp = bmp180.readTemperature(); 
//            System.out.println(">>> Temperature is now :" + NF.format(temp) + "\272C");                          
              if (Math.abs(temp - originalTemp) > 0.25f)
              {
                if (temp > originalTemp)
                  System.out.println(">>> Warning! >>> " + SDF.format(new Date()) + ", Temperature is rising, now " + NF.format(temp) + "\272C");                
                else
                  System.out.println("                 Setting base temperature to " + NF.format(temp) + "\272C");
                originalTemp = temp;
              }
            } 
            catch (Exception ex) 
            { 
              System.err.println(ex.getMessage()); 
              ex.printStackTrace();
            }
          }
        }
      };
    tempReader.start();
    
    final Thread me = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread()
       {
         public void run()
         {
           System.out.println();
           sac.quit();
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
}
