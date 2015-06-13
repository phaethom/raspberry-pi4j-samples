package weatherstation.logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * REST Interface to MySQL
 * JSON payload looks like:
 *
 *  { "dir": 350.0,
 *    "volts": 3.4567,
 *    "speed": 12.345,
 *    "gust": 13.456,
 *    "rain": 0.1,
 *    "press": 101300.00,
 *    "temp": 18.34,
 *    "hum": 58.5,
 *    "cputemp": 34.56 }
 */
public class MySQLLogger implements LoggerInterface
{
  private long lastLogged = 0L; // Time of the last logging
  private static final long MINIMUM_BETWEEN_LOGS = 5000L;
  private final static NumberFormat DOUBLE_FMT = new DecimalFormat("#0.000");
  private final static String REST_URL = "http://tamere.la.naine/pousse";

  private String json2qs(JSONObject json, String jMember, String qsName)
  {
    String ret = null;
    try
    {
      Object o = json.get(jMember);
      if (o != null)
      {
        if (o instanceof Double)
        {
          double d = ((Double)o).doubleValue();
          ret = qsName + "=" + DOUBLE_FMT.format(d);
        }
        else
          System.out.println("Got a " + o.getClass().getName());
      }
      else
        System.out.println("No " + jMember);
    }
    catch (JSONException je) { /* No there */ }
    return ret;
  }
  
  private String composeQS(JSONObject json)
  {
    String qs = "";
    String s = json2qs(json, "cputemp", "CPU");
    if (s != null)
      qs += ((qs.trim().length() == 0 ? "" : "&") + s);
    s = json2qs(json, "dir", "WDIR");
    if (s != null)
      qs += ((qs.trim().length() == 0 ? "" : "&") + s);
    s = json2qs(json, "speed", "WSPEED");
    if (s != null)
      qs += ((qs.trim().length() == 0 ? "" : "&") + s);
    s = json2qs(json, "gust", "WGUST");
    if (s != null)
      qs += ((qs.trim().length() == 0 ? "" : "&") + s);
    s = json2qs(json, "rain", "RAIN");
    if (s != null)
      qs += ((qs.trim().length() == 0 ? "" : "&") + s);
    s = json2qs(json, "press", "PRMSL");
    if (s != null)
      qs += ((qs.trim().length() == 0 ? "" : "&") + s);
    s = json2qs(json, "temp", "ATEMP");
    if (s != null)
      qs += ((qs.trim().length() == 0 ? "" : "&") + s);
    s = json2qs(json, "hum", "HUM");
    if (s != null)
      qs += ((qs.trim().length() == 0 ? "" : "&") + s);
    
    return qs;
  }
  
  @Override
  public void pushMessage(JSONObject json)
    throws Exception
  {
    long now = System.currentTimeMillis();
    if (now - this.lastLogged > MINIMUM_BETWEEN_LOGS)
    {
      System.out.print(" >>> Logging... ");
      String queryString = composeQS(json);        
      this.lastLogged = now;
      // TASK Actual logging goes here
      
      System.out.println("REST Request:" + REST_URL + "?" + queryString);
    }
  }
}
