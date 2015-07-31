package i2c.samples;

import i2c.servo.PCA9685;

/*
 * Standard, all the way, clockwise, counterclockwise
 */
public class DemoStandard
{
  private static void waitfor(long howMuch)
  {
    try { Thread.sleep(howMuch); } catch (InterruptedException ie) { ie.printStackTrace(); }
  }

  public static void main(String[] args)
  {
    PCA9685 servoBoard = new PCA9685();
    int freq = 60;
    servoBoard.setPWMFreq(freq); // Set frequency in Hz
    
//  final int CONTINUOUS_SERVO_CHANNEL = 14;
    final int STANDARD_SERVO_CHANNEL   = 13; // 15
    
    int servo = STANDARD_SERVO_CHANNEL;
    int servoMin = 122; 
    int servoMax = 615; 
    int diff = servoMax - servoMin;
    System.out.println("Min:" + servoMin + ", Max:" + servoMax + ", diff:" + diff);
    
    try
    {
      servoBoard.setPWM(servo, 0, 0);   // Stop the standard one
      waitfor(2000);
      System.out.println("Let's go, 1 by 1");
      for (int i=servoMin; i<=servoMax; i++)
      {
        System.out.println("i=" + i + ", " + (-90f + (((float)(i - servoMin) / (float)diff) * 180f)));
        servoBoard.setPWM(servo, 0, i);
        waitfor(10);
      } 
      for (int i=servoMax; i>=servoMin; i--)
      {
        System.out.println("i=" + i + ", " + (-90f + (((float)(i - servoMin) / (float)diff) * 180f)));
        servoBoard.setPWM(servo, 0, i);
        waitfor(10);
      } 
      servoBoard.setPWM(servo, 0, 0);   // Stop the standard one
      waitfor(2000);
      System.out.println("Let's go, 1 deg by 1 deg");
      for (int i=servoMin; i<=servoMax; i+=(diff / 180))
      {
        System.out.println("i=" + i + ", " + Math.round(-90f + (((float)(i - servoMin) / (float)diff) * 180f)));
        servoBoard.setPWM(servo, 0, i);
        waitfor(10);
      } 
      for (int i=servoMax; i>=servoMin; i-=(diff / 180))
      {
        System.out.println("i=" + i + ", " + Math.round(-90f + (((float)(i - servoMin) / (float)diff) * 180f)));
        servoBoard.setPWM(servo, 0, i);
        waitfor(10);
      } 
      servoBoard.setPWM(servo, 0, 0);   // Stop the standard one
      waitfor(2000);
      
      float[] degValues = { -10, 0, -90, 45, -30, 90, 10, 20, 30, 40, 50, 60, 70, 80, 90, 0 };
      for (float f : degValues)
      {
        int pwm = degreeToPWM(servoMin, servoMax, f);
        System.out.println(f + " degrees (" + pwm + ")");
        servoBoard.setPWM(servo, 0, pwm);
        waitfor(1500);
      }
    }
    finally
    {
      servoBoard.setPWM(servo, 0, 0);   // Stop the standard one
    }
    
    System.out.println("Done.");
  }
  
  /*
   * deg in [-90..90]
   */
  private static int degreeToPWM(int min, int max, float deg)
  {
    int diff = max - min;
    float oneDeg = diff / 180f;
    return Math.round(min + ((deg + 90) * oneDeg));
  }    
}
