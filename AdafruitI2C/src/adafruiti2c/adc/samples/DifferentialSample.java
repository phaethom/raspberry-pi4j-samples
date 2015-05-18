package adafruiti2c.adc.samples;

import adafruiti2c.adc.AdafruitADS1x15;

public class DifferentialSample
{
  public static void main(String[] args)
  {
    final AdafruitADS1x15 adc = new AdafruitADS1x15(AdafruitADS1x15.ICType.IC_ADS1115);
    int gain = 4096;
    int sps  =  250;

    float volt2 = adc.readADCSingleEnded(AdafruitADS1x15.Channels.CHANNEL_2, gain, sps) / 1000;
    float volt3 = adc.readADCSingleEnded(AdafruitADS1x15.Channels.CHANNEL_3, gain, sps) / 1000;
    
    float voltDiff = adc.readADCDifferential23(gain, sps) / 1000;
    System.out.printf("%.8f %.8f %.8f %.8f \n", volt2, volt3, (volt3 - volt2), -voltDiff);
  }
}
