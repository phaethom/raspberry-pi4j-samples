package raspisamples.adc.levelreader;

import adc.ADCContext;
import adc.ADCListener;
import adc.ADCObserver;

import adc.utils.EscapeSeq;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.fusesource.jansi.AnsiConsole;

public class ADCChannels_1_to_8
{
  private final static boolean DEBUG = "true".equals(System.getProperty("verbose", "false"));
  private final static int THRESHOLD = Integer.parseInt(System.getProperty("threshold", "45"));
  
  private final static NumberFormat DF3 = new DecimalFormat("##0");
  private final static NumberFormat DF4 = new DecimalFormat("###0");
  
  private static ADCObserver.MCP3008_input_channels channel[] = null;
  private final int[] channelValues;
  private final int[] channelVolumes;
  
  private int currentLevel = 0;

  private final ADCObserver obs;
  
  public ADCChannels_1_to_8(ADCObserver.MCP3008_input_channels[] ADCInput) throws Exception
  {
    channel = ADCInput;
    obs = new ADCObserver(channel);
    channelValues = new int[channel.length];
    channelVolumes = new int[channel.length];
    for (int i=0; i<channel.length; i++)
      channelValues[i] = channelVolumes[i] = 0;
    
    ADCContext.getInstance().addListener(new ADCListener()
       {
         @Override
         public void valueUpdated(ADCObserver.MCP3008_input_channels inputChannel, int newValue) 
         {
           int ch = inputChannel.ch();
           int volume = (int)(newValue / 10.23); 
           channelValues[ch]  = newValue; 
           channelVolumes[ch] = volume;
           if (DEBUG) // A table, with ansi box-drawing characters. Channel, volume (%), value.
           {
             if (false)
             {
               System.out.println("readAdc:" + Integer.toString(newValue) + 
                                               " (0x" + lpad(Integer.toString(newValue, 16).toUpperCase(), "0", 2) + 
                                               ", 0&" + lpad(Integer.toString(newValue, 2), "0", 8) + ")"); 
               String output = "";
               for (int chan=0; chan<channel.length; chan++)
                 output += (channelVolumes[chan] > THRESHOLD ? "*" : " ");
               output += " || ";
               for (int chan=0; chan<channel.length; chan++)
                 output += (Integer.toString(chan) + ":" + lpad(Integer.toString(channelVolumes[chan]), " ", 4) + (chan != (channel.length - 1)?" | ":" |"));
               System.out.println(output);
             }
             AnsiConsole.out.println(EscapeSeq.ANSI_CLS);
             AnsiConsole.out.println(EscapeSeq.ansiLocate(1, 1));
             boolean ansiBox = false;
             // See http://en.wikipedia.org/wiki/Box-drawing_character
             String str = (ansiBox ? "\u2554\u2550\u2550\u2550\u2564\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2564\u2550\u2550\u2550\u2550\u2550\u2550\u2557" :
                                     "+---+-------+------+");
             AnsiConsole.out.println(str);
             for (int chan=0; chan<channel.length; chan++)
             {
               str = (ansiBox ? "\u2551 " : "| ") + 
                     Integer.toString(chan) + (ansiBox ? " \u2503 " : " | ") +
                     lpad(DF3.format(channelVolumes[chan]), " ", 3) + (ansiBox ? " % \u2503 " : " % | ") +
                     lpad(DF4.format(channelValues[chan]), " ", 4) + (ansiBox ? " \u2551" : " |");
               AnsiConsole.out.println(str);
             }
             str = (ansiBox ? "\u255a\u2550\u2550\u2550\u2567\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2567\u2550\u2550\u2550\u2550\u2550\u2550\u255d" :
                              "+---+-------+------+");
             AnsiConsole.out.println(str);
           }

           int maxLevel = 0;
           for (int chan=0; chan<channel.length; chan++)
           {
             if (channelVolumes[chan] > THRESHOLD)
               maxLevel = Math.max(chan+1, maxLevel);
           }
           if (maxLevel != currentLevel)
           {
             System.out.print("Level : " + maxLevel + " ");
             for (int i=0; i<maxLevel; i++)
               System.out.print(">>");
             System.out.println();
             currentLevel = maxLevel;
           }
         }
       });
    System.out.println("Start observing.");
    Thread observer = new Thread()
      {
        public void run()
        {
          obs.start(0L);
        }
      };
    observer.start();         
  }
  
  public void quit()
  {
    System.out.println("Stop observing.");
    if (obs != null)
      obs.stop();    
  }

  private static String lpad(String str, String with, int len)
  {
    String s = str;
    while (s.length() < len)
      s = with + s;
    return s;
  }
  
  /**
   * Sample main
   * @param args Unused. But see the "threshold" and "verbose" System variables.
   * @throws Exception ...in case there is a problem.
   */
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
