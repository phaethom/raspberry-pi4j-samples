package raspisamples.gpio;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class GPIOLevelReader
{
  private final static GpioController gpio = GpioFactory.getInstance();
  private final boolean[] status = { false, false, false, false, false, false, false };
  
  public GPIOLevelReader()
  {
    this.init();  
  }
  
  public void displayStatus()
  {
    String statusStr = "";
    for (boolean b : status)
      statusStr += (b?"*":" ");
    System.out.println(statusStr);
  }
  
  public void init()
  {
    final GpioPinDigitalInput contact00 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_DOWN);
    contact00.addListener(new GpioPinListenerDigital() 
      {
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) 
        {
//        System.out.println(" --> GPIO pin state changed: " + event.getPin() + " = " + event.getState());
          status[0] = (event.getState().isHigh());
          displayStatus();
        }
      });
    final GpioPinDigitalInput contact01 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_DOWN);
    contact01.addListener(new GpioPinListenerDigital() 
      {
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) 
        {
//        System.out.println(" --> GPIO pin state changed: " + event.getPin() + " = " + event.getState());
          status[1] = (event.getState().isHigh());
          displayStatus();
        }
      });
    final GpioPinDigitalInput contact02 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
    contact02.addListener(new GpioPinListenerDigital() 
      {
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) 
        {
//        System.out.println(" --> GPIO pin state changed: " + event.getPin() + " = " + event.getState());
          status[2] = (event.getState().isHigh());
          displayStatus();
        }
      });
    final GpioPinDigitalInput contact03 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.PULL_DOWN);
    contact03.addListener(new GpioPinListenerDigital() 
      {
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) 
        {
//        System.out.println(" --> GPIO pin state changed: " + event.getPin() + " = " + event.getState());
          status[3] = (event.getState().isHigh());
          displayStatus();
        }
      });
    final GpioPinDigitalInput contact04 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN);
    contact04.addListener(new GpioPinListenerDigital() 
      {
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) 
        {
//        System.out.println(" --> GPIO pin state changed: " + event.getPin() + " = " + event.getState());
          status[4] = (event.getState().isHigh());
          displayStatus();
}
      });
    final GpioPinDigitalInput contact05 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05, PinPullResistance.PULL_DOWN);
    contact05.addListener(new GpioPinListenerDigital() 
      {
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) 
        {
//        System.out.println(" --> GPIO pin state changed: " + event.getPin() + " = " + event.getState());
          status[5] = (event.getState().isHigh());
          displayStatus();
        }
      });
    final GpioPinDigitalInput contact06 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_06, PinPullResistance.PULL_DOWN);
    contact06.addListener(new GpioPinListenerDigital() 
      {
        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) 
        {
//        System.out.println(" --> GPIO pin state changed: " + event.getPin() + " = " + event.getState());
          status[6] = (event.getState().isHigh());
          displayStatus();
        }
      });
  }
  
  public static void main(String[] args)
  {
    final Thread me = Thread.currentThread();
    
    Runtime.getRuntime().addShutdownHook(new Thread()
     {
       public void run()
       {
         gpio.shutdown();
         synchronized (me)
         {
           me.notify();
         }
       }
     });
    
/*  GPIOLevelReader gpioLevelReader = */ new GPIOLevelReader();
    
    synchronized (me)
    {
      try 
      { 
        me.wait(); 
        System.out.println("\nDone!\n=================\n");
      } catch (Exception ex) 
      {
        ex.printStackTrace();
      }
    }
  }
}
