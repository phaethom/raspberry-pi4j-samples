package breadboard.game;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class ReflexGame
{
  private static long before = 0L;
  private static long after  = 0L;
  private static Thread waiter = null;
  
  public static void main(String[] args)
  {
    final GpioController gpio = GpioFactory.getInstance();

    // provision gpio pin #01 as an output pin and turn on
    final GpioPinDigitalOutput led   = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "TheLED", PinState.LOW);
    final GpioPinDigitalInput button = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
    button.addListener(new GpioPinListenerDigital() 
      {
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) 
        {
          if (event.getState().isHigh())
            buttonHit();
        }
      });
    
    System.out.println("Get ready...");
    long rnd = (10000L * Math.round(1 - Math.random()));
    delay(rnd);

    // turn on the led
    led.high();
    System.out.println("Hit the button NOW!!");
    before = System.currentTimeMillis();
    
    waiter = Thread.currentThread();
    synchronized (waiter)
    {
      try { waiter.wait(); }
      catch (InterruptedException ex) {}
    }
    System.out.println("Good Job!");
    led.low();
    gpio.shutdown();
    System.exit(0);
  }
  
  private static void buttonHit()
  {
    after = System.currentTimeMillis();
    System.out.println("It took you " + Long.toString(after - before) + " ms.");
    synchronized (waiter)
    {
      waiter.notify();
    }
  }
  
  private static void delay(long ms)
  {
    try { Thread.sleep(ms); } catch (InterruptedException ie) {}
  }
}
