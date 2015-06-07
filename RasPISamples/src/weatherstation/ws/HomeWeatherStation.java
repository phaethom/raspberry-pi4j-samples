package weatherstation.ws;

import com.pi4j.system.SystemInfo;

import java.nio.channels.NotYetConnectedException;

import org.json.JSONObject;

import weatherstation.SDLWeather80422;

/**
 * The real project
 * 
 * Loops every 1000 ms, reads data from the SDL 80422:
 * - Wind:
 *   - Speed (in km/h)
 *   - Direction
 *   - Gust (in km/h)
 *   - Volts
 *   - Rain (in mm)
 * - BMP180: (if available)
 *   - Temperature (in Celcius)
 *   - Pressure (in Pa)
 * - HTU21DF: (if available)
 *   - Relative Humidity (%)
 * - CPU Temperature (in Celcius)
 * 
 * Feeds a WebSocket server with a json object like 
 *  { "dir": 350.0,
 *    "volts": 3.4567,
 *    "speed": 12.345,
 *    "gust": 13.456,
 *    "rain": 0.1,
 *    "press": 101300.00,
 *    "temp": 18.34,
 *    "hum": 58.5,
 *    "cputemp": 34.56 }
 *    
 *  TODO
 *    - Logging
 *    - Sending Data to some DB (REST interface)
 *    - Add orientable camera
 *  
 *  Use -Dws.verbose=true for more output.
 */
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
      float rain = weatherStation.getCurrentRainTotal();
      JSONObject windObj = new JSONObject();
      windObj.put("dir", wd);
      windObj.put("volts", volts);
      windObj.put("speed", ws);
      windObj.put("gust", wg);
      windObj.put("rain", rain);
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
      float cpuTemp = SystemInfo.getCpuTemperature();
      windObj.put("cputemp", cpuTemp);
      /*
       * Sample message:
       * { "dir": 350.0,
       *   "volts": 3.4567,
       *   "speed": 12.345,
       *   "gust": 13.456,
       *   "rain": 0.1,
       *   "press": 101300.00,
       *   "temp": 18.34,
       *   "hum": 58.5,
       *   "cputemp": 34.56 }
       */
      try
      {
        String message = windObj.toString();
        if ("true".equals(System.getProperty("ws.verbose", "false")))
          System.out.println("-> Sending " + message);
        wsf.pushMessage(message);
      }
      catch (NotYetConnectedException nyce)
      {
        System.err.println(" ... Not yet connected, check your WebSocket server");
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
      
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
