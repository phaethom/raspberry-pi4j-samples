package adafruiti2c.adc.samples;

import adafruiti2c.adc.AdafruitADS1x15;

public class SingleEndedSample
{
  public static void main(String[] args)
  {
    final AdafruitADS1x15 adc = new AdafruitADS1x15(AdafruitADS1x15.ICType.IC_ADS1115);
    int gain = 4096;
    int sps  =  250;
    float value = adc.readADCSingleEnded(AdafruitADS1x15.Channels.CHANNEL_2, gain, sps);
    System.out.printf("%.6f\n", (value / 1000f));
  }
}
