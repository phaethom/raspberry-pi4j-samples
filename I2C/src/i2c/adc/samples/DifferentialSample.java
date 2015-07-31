package i2c.adc.samples;

import i2c.adc.ADS1x15;

public class DifferentialSample
{
  public static void main(String[] args)
  {
    final ADS1x15 adc = new ADS1x15(ADS1x15.ICType.IC_ADS1115);
    int gain = 4096;
    int sps  =  250;

    float volt2 = adc.readADCSingleEnded(ADS1x15.Channels.CHANNEL_2, gain, sps) / 1000;
    float volt3 = adc.readADCSingleEnded(ADS1x15.Channels.CHANNEL_3, gain, sps) / 1000;
    
    float voltDiff = adc.readADCDifferential23(gain, sps) / 1000;
    System.out.printf("%.8f %.8f %.8f %.8f \n", volt2, volt3, (volt3 - volt2), -voltDiff);
  }
}
