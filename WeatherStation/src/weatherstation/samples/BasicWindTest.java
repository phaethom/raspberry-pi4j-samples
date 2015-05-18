package weatherstation.samples;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

import java.text.DecimalFormat;
import java.text.Format;

import weatherstation.SDLWeather80422;

import weatherstation.SDLWeather80422.AdcMode;
import weatherstation.SDLWeather80422.SdlMode;

public class BasicWindTest
{
  
  private final static Format SPEED_FMT = new DecimalFormat("##0.00");
  private final static Format VOLTS_FMT = new DecimalFormat("##0.000");
  private final static Format DIR_FMT   = new DecimalFormat("##0.0");
  
  // Sample main, for tests
  private static boolean go = true;
  public static void main(String[] args)
  {
    Runtime.getRuntime().addShutdownHook(new Thread()
     {
       public void run()
       {
         System.out.println("\nUser interrupted.");
         go = false;
         try { Thread.sleep(1100L); } catch (Exception ex) { ex.printStackTrace(); }
       }
     });
      
    SDLWeather80422 weatherStation = new SDLWeather80422();
    final Pin ANEMOMETER_PIN = RaspiPin.GPIO_16; // <- WiringPi number. aka GPIO 15, #10
    final Pin RAIN_PIN       = RaspiPin.GPIO_01; // <- WiringPi number. aka GPIO 18, #12
    weatherStation.init(ANEMOMETER_PIN, RAIN_PIN, AdcMode.SDL_MODE_I2C_ADS1015);
    weatherStation.setWindMode(SdlMode.SAMPLE, 5);
    
    while (go)
    {
      double ws = weatherStation.currentWindSpeed();
      double wg = weatherStation.getWindGust();
      float wd = weatherStation.getCurrentWindDirection();
      double volts = weatherStation.getCurrentWindDirectionVoltage();
      
      System.out.println("Wind : Dir=" + DIR_FMT.format(wd) + "\272, (" + VOLTS_FMT.format(volts) + " V) Speed:" + 
                                         SPEED_FMT.format(SDLWeather80422.toKnots(ws)) + " kts, Gust:" + 
                                         SPEED_FMT.format(SDLWeather80422.toKnots(wg)) + " kts");
      try { Thread.sleep(1000L); } catch (Exception ex) { ex.printStackTrace(); }
    }
    weatherStation.shutdown();
    System.out.println("Done.");
  }
}
