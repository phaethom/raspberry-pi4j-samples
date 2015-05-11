package weatherstation;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.wiringpi.Gpio;

public class SDLWeather80422
{
  final GpioController gpio = GpioFactory.getInstance();
  
  private final static int SDL_MODE_INTERNAL_AD = 0;
  private final static int SDL_MODE_I2C_ADS1015 = 1;

  // sample mode means return immediately.  
  // The wind speed is averaged at sampleTime or when you ask, whichever is longer
  private final static int SDL_MODE_SAMPLE = 0;
  // Delay mode means to wait for sampleTime and the average after that time.
  private final static int SDL_MODE_DELAY = 1;

  private final static double WIND_FACTOR = 2.400;

  private int currentWindCount = 0;
  private int currentRainCount = 0;
  private int shortestWindTime = 0;

  private GpioPinDigitalInput pinAnem;
  private GpioPinDigitalInput pinRain;
  private int intAnem = 0;
  private int intRain = 0;
  private int ADChannel = 0;
  private int ADMode = 0;

  private double currentWindSpeed     = 0.0;
  private double currentWindDirection = 0.0;

  private int lastWindTime = 0;
                   
  private double sampleTime = 5.0;
  private int selectedMode = SDL_MODE_SAMPLE;
  private int startSampleTime = 0;

  private int currentRainMin = 0;
  private int lastRainTime = 0;

  private int ads1015 = 0;

  public SDLWeather80422()
  {
    super();
  }
  
  // RaspiPin.GPIO_05
  public void init(Pin anemo, Pin rain, int intRain, int ADMode)
  {
    this.pinAnem = gpio.provisionDigitalInputPin(anemo, "Anemometer");
    this.pinRain = gpio.provisionDigitalInputPin(rain,  "Rainmeter");
    
//  Gpio.add_event_detect(pinAnem, GPIO.RISING, callback=self.serviceInterruptAnem, bouncetime=300)  
//  GPIO.add_event_detect(pinRain, GPIO.RISING, callback=self.serviceInterruptRain, bouncetime=300)  


  }
}
