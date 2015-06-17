package breadboard.game;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * Implements the nuts and bolts of the reflex game.
 * No need to worry about that in the main class.
 * From the main:
 *   Invoke the initCtx method
 *   Invoke the go method
 *     in the onButtonPressed method, invoke the release method
 *   Invoke the freeResources method
 */
public class ReflexGameMaster
{
  private static long startedAt = 0L;
  private static Thread waiter = null;
  private final static long MAX_WAIT_TIME = 10000L; // 10 sec max.

  private final GpioController gpio = GpioFactory.getInstance();
  private GpioPinDigitalOutput led   = null;
  private GpioPinDigitalInput button = null;

  private PushButtonObserver pbo = null;
  
  public ReflexGameMaster(PushButtonObserver obs)
  {
    if (obs == null)
      throw new IllegalArgumentException("Obvserser cannot be null");
    this.pbo = obs;
  }

  public void initCtx()
  {
    initCtx(RaspiPin.GPIO_01, RaspiPin.GPIO_02);
  }

  public void initCtx(Pin ledPin, Pin buttonPin)
  {
    // provision gpio pin #01 as an output pin and turn it off
    led = gpio.provisionDigitalOutputPin(ledPin, "TheLED", PinState.LOW);
    button = gpio.provisionDigitalInputPin(buttonPin, PinPullResistance.PULL_DOWN);
    button.addListener(new GpioPinListenerDigital() {
      @Override
      public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        if (event.getState().isHigh())
          pbo.onButtonPressed();
      }
    });
  }

  public void release()
  {
    Thread waiter = this.getWaiter();
    synchronized (waiter)
    {
      waiter.notify();
    }
  }

  public long getStartTime()
  {
    return this.startedAt;
  }

  public void go()
  {
    System.out.println("Get ready...");
    long rnd = (MAX_WAIT_TIME * Math.round(1 - Math.random())); // TASK Parameter this amount?
    delay(rnd);

    // turn on the led
    led.high();
    System.out.println("Hit the button NOW!!");
    startedAt = System.currentTimeMillis();

    waiter = Thread.currentThread();
    synchronized (waiter)
    {
      try
      {
        waiter.wait();
      }
      catch (InterruptedException ex)
      {
        ex.printStackTrace();
      }
    }
    System.out.println("Good Job!");
    led.low();
  }

  public void freeResources()
  {
    gpio.shutdown();
    System.exit(0);
  }

  private Thread getWaiter()
  {
    return waiter;
  }

  private static void delay(long ms)
  {
    try { Thread.sleep(ms); } catch (InterruptedException ie) { ie.printStackTrace(); }
  }
}
