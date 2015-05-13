package weatherstation.utils;

public class Utilities
{
  private final static double VARY_VALUE = 0.05d;
  
  public static boolean fuzzyCompare(double thisValue, double thatValue)
  {
    boolean b = false;
    if (thatValue > (thisValue * (1.0 - VARY_VALUE)) && 
        thatValue < (thisValue * (1.0 + VARY_VALUE)))
      b = true;
    return b;
  }
    
  public enum Voltage
  {
    V3_3(0.66f),
    V5(1.0f);

    private final float adjust;
    Voltage(float adjust)
    {
      this.adjust = adjust;
    }
    
    public float adjust() { return this.adjust; }
  }
  
  private static double getAdjustment(Voltage v)
  {
    return v.adjust();
  }
  
  public static double voltageToDegrees(double value, double defaultWindDir)
  {
    return voltageToDegrees(value, defaultWindDir, Voltage.V3_3);
  }
  
  public static double voltageToDegrees(double value, double defaultWindDir, Voltage v)
  {
    if (fuzzyCompare(3.84 * getAdjustment(v), value))
      return 0d;
    if (fuzzyCompare(1.98 * getAdjustment(v), value))
      return 22.5;
    if (fuzzyCompare(2.25 * getAdjustment(v), value))
      return 45;                     
    if (fuzzyCompare(0.41 * getAdjustment(v), value))
      return 67.5;                     
    if (fuzzyCompare(0.45 * getAdjustment(v), value))
      return 90.0;
    if (fuzzyCompare(0.32 * getAdjustment(v), value))
      return 112.5;
    if (fuzzyCompare(0.90 * getAdjustment(v), value))
      return 135.0;
    if (fuzzyCompare(0.62 * getAdjustment(v), value))
      return 157.5;
    if (fuzzyCompare(1.40 * getAdjustment(v), value))
      return 180;
    if (fuzzyCompare(1.19 * getAdjustment(v), value))
      return 202.5;
    if (fuzzyCompare(3.08 * getAdjustment(v), value))
      return 225;
    if (fuzzyCompare(2.93 * getAdjustment(v), value))
      return 247.5;
    if (fuzzyCompare(4.62 * getAdjustment(v), value))
      return 270.0;
    if (fuzzyCompare(4.04 * getAdjustment(v), value))
      return 292.5;
    if (fuzzyCompare(4.34 * getAdjustment(v), value)) // chart in manufacturers documentation wrong
      return 315.0;
    if (fuzzyCompare(3.43 * getAdjustment(v), value))
      return 337.5;
    
    return defaultWindDir;
  }
  
  public static long currentTimeMicros()
  {
    long milli = System.currentTimeMillis();
    return milli * 1000;
  }
}
