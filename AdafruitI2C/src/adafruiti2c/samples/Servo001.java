package adafruiti2c.samples;

import adafruiti2c.AdafruitPCA9685;

public class Servo001
{
  public static void main(String[] args)
  {
    AdafruitPCA9685 servoBoard = new AdafruitPCA9685();
    servoBoard.setPWMFreq(60); // Set frequency to 60 Hz
    int servoMin = 150;        // Min pulse length out of 4096
    int servoMax = 600;        // Max pulse length out of 4096
    
    final int CONTINUOUS_SERVO_CHANNEL = 14;
    final int STANDARD_SERVO_CHANNEL   = 15;
    
    for (int i=0; i<10; i++)
    {
      System.out.println("i=" + i);
      servoBoard.setPWM(STANDARD_SERVO_CHANNEL,   0, servoMin);
      servoBoard.setPWM(CONTINUOUS_SERVO_CHANNEL, 0, servoMin);
      waitfor(1000);
      servoBoard.setPWM(STANDARD_SERVO_CHANNEL,   0, servoMax);
      servoBoard.setPWM(CONTINUOUS_SERVO_CHANNEL, 0, servoMax);
      waitfor(1000);
    } 
    servoBoard.setPWM(CONTINUOUS_SERVO_CHANNEL, 0, 0); // Stop the continuous one
    System.out.println("Done with the demo.");
  }
  
  private static void waitfor(long howMuch)
  {
    try { Thread.sleep(howMuch); } catch (InterruptedException ie) { ie.printStackTrace(); }
  }
}
