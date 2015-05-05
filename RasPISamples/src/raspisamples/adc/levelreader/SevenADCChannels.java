package raspisamples.adc.levelreader;

import adc.ADCContext;
import adc.ADCListener;
import adc.ADCObserver;

public class SevenADCChannels
{
  private final static boolean DEBUG = false;
  
  private static ADCObserver.MCP3008_input_channels channel[] = null;
  private final int[] channelValues = new int[] { 0, 0, 0, 0, 0, 0, 0 };
  
  public SevenADCChannels() throws Exception
  {
    channel = new ADCObserver.MCP3008_input_channels[] 
    {
      ADCObserver.MCP3008_input_channels.CH0,
      ADCObserver.MCP3008_input_channels.CH1,
      ADCObserver.MCP3008_input_channels.CH2,  
      ADCObserver.MCP3008_input_channels.CH3,  
      ADCObserver.MCP3008_input_channels.CH4, 
      ADCObserver.MCP3008_input_channels.CH5, 
      ADCObserver.MCP3008_input_channels.CH6 
    };
    final ADCObserver obs = new ADCObserver(channel);
    
    ADCContext.getInstance().addListener(new ADCListener()
       {
         @Override
         public void valueUpdated(ADCObserver.MCP3008_input_channels inputChannel, int newValue) 
         {
//         if (inputChannel.equals(channel))
           {
             int ch = inputChannel.ch();
             int volume = (int)(newValue / 10.23); // [0, 1023] ~ [0x0000, 0x03FF] ~ [0&0, 0&1111111111]
             channelValues[ch] = volume;
             if (DEBUG)
               System.out.println("readAdc:" + Integer.toString(newValue) + 
                                               " (0x" + lpad(Integer.toString(newValue, 16).toUpperCase(), "0", 2) + 
                                               ", 0&" + lpad(Integer.toString(newValue, 2), "0", 8) + ")"); 
             String output = "";
             for (int chan=0; chan<channel.length; chan++)
               output += (channelValues[chan] > 80 ? "X" : " ");
             output += "  ";
             for (int chan=0; chan<channel.length; chan++)
               output += "Ch " + Integer.toString(chan) + ", Vol:" + lpad(Integer.toString(channelValues[chan]), " ", 3) + "%" + (chan != (channel.length - 1)?", ":"");
             System.out.println(output);
           }
         }
       });
    obs.start();         
    
    Runtime.getRuntime().addShutdownHook(new Thread()
       {
         public void run()
         {
           if (obs != null)
             obs.stop();
         }
       });    
  }
  
  public static void main(String[] args) throws Exception
  {
    // Channels are hard-coded
    new SevenADCChannels();
  }

  private static String lpad(String str, String with, int len)
  {
    String s = str;
    while (s.length() < len)
      s = with + s;
    return s;
  }
}
