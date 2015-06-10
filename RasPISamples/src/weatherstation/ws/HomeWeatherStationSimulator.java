package weatherstation.ws;

import org.json.JSONObject;

import weatherstation.logger.LoggerInterface;

/**
 * Use this when the Weather Staiton is not available (ie you're not on the RPi)
 */
public class HomeWeatherStationSimulator
{
  private static boolean go = true;
  private static LoggerInterface logger = null;
  
  public static void main(String[] args) throws Exception
  {
    final Thread coreThread = Thread.currentThread();
    final WebSocketFeeder wsf = new WebSocketFeeder();
    String loggerClassName = System.getProperty("data.logger", null);
    if (loggerClassName != null)
    {
      try
      { 
        Class<? extends LoggerInterface> logClass = Class.forName(loggerClassName).asSubclass(LoggerInterface.class);
        logger = logClass.newInstance();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }

    Runtime.getRuntime().addShutdownHook(new Thread()
     {
       public void run()
       {
         System.out.println("\nUser interrupted.");
         go = false;
         synchronized (coreThread)
         {
           coreThread.notify();
         }
       }
     });
      
    double windSpeed = 0d;
    double windGust = 0d;
    float windDir = 0f;
    double voltage = 0;
    double pressure = 101300;
    double humidity = 50;
    double temperature = 15;
    
    while (go)
    {
      double ws = generateRandomValue(windSpeed, 3, 0, 65);
      double wg = generateRandomValue(windGust,  5, 0, 65);
      float wd  = (float)generateRandomValue(windDir, 10, 0, 360);
      double volts = generateRandomValue(voltage, 3, 0, 65);
      float temp = (float)generateRandomValue(temperature, 2, -10, 50);
      float press = (float)generateRandomValue(pressure, 1, 98000, 105000);
      float hum = (float)generateRandomValue(humidity, 0.1, 0, 100);
      JSONObject windObj = new JSONObject();
      windObj.put("dir", wd);
      windObj.put("volts", volts);
      windObj.put("speed", ws);
      windObj.put("gust", wg);
      windObj.put("temp", temp);
      windObj.put("press", press);
      windObj.put("hum", hum);
      /*
       * Sample message:
       * { "dir": 350.0,
       *   "volts": 3.4567,
       *   "speed": 12.345,
       *   "gust": 13.456,
       *   "press": 101300.00,
       *   "temp": 18.34,
       *   "hum": 58.5 }
       */
      System.out.println("Pushing " + windObj.toString());
      try { wsf.pushMessage(windObj.toString()); } catch (Exception ex) {}
      if (logger != null)
      {
        try
        {
          logger.pushMessage(windObj);
        }
        catch (Exception ex) 
        {
          ex.printStackTrace();
        }
      }
      windSpeed = ws;
      windGust = wg;
      windDir = wd;
      voltage = volts;
      pressure = press;
      temperature = temp;
      humidity= hum;
      
      try 
      { 
        synchronized (coreThread)
        {
          coreThread.wait(1000L); 
        }
      } 
      catch (Exception ex) { ex.printStackTrace(); }
    }
    wsf.shutdown();
    System.out.println("Done.");
  }
  
  public static double generateRandomValue(double from, double diffRange, double min, double max)
  {
    double d = from;
    while (true)
    {
      double rnd = 0.5 - Math.random();
      rnd *= diffRange;
      if (d + rnd >= min && d + rnd <= max)
      {
        d += rnd;
        break;
      }
    }
    return d;
  }
}
