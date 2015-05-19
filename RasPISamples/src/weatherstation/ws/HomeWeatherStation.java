package weatherstation.ws;

import org.json.JSONObject;

import weatherstation.SDLWeather80422;

public class HomeWeatherStation
{
  private static boolean go = true;
  public static void main(String[] args) throws Exception
  {
    final Thread coreThread = Thread.currentThread();
    final WebSocketFeeder wsf = new WebSocketFeeder();

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
      
    SDLWeather80422 weatherStation = new SDLWeather80422(); // With default parameters.
    weatherStation.setWindMode(SDLWeather80422.SdlMode.SAMPLE, 5);
        
    while (go)
    {
      double ws = weatherStation.currentWindSpeed();
      double wg = weatherStation.getWindGust();
      float wd  = weatherStation.getCurrentWindDirection();
      double volts = weatherStation.getCurrentWindDirectionVoltage();
      JSONObject windObj = new JSONObject();
      windObj.put("dir", wd);
      windObj.put("volts", volts);
      windObj.put("speed", ws);
      windObj.put("gust", wg);
      // Add temperature, pressure, humidity
      if (weatherStation.isBMP180Available())
      {
        try
        {
          float temp = weatherStation.readTemperature();
          float press = weatherStation.readPressure();
          windObj.put("temp", temp);
          windObj.put("press", press);
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
      if (weatherStation.isHTU21DFAvailable())
      {
        try
        {
          float hum = weatherStation.readHumidity();
          windObj.put("hum", hum);
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }      
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
      wsf.pushMessage(windObj.toString());
      
      try 
      { 
        synchronized (coreThread)
        {
          coreThread.wait(1000L); 
        }
      } 
      catch (Exception ex) { ex.printStackTrace(); }
    }
    weatherStation.shutdown();
    wsf.shutdown();
    System.out.println("Done.");
  }
}
