package i2c.samples;

import i2c.servo.PCA9685;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/*
 * Continuous servo
 * Parallax Futaba S148
 * 
 * Enter the speed interactively, and see for yourself.
 */
public class Servo003
{
  private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

  public static String userInput(String prompt)
  {
    String retString = "";
    System.err.print(prompt);
    try
    {
      retString = stdin.readLine();
    }
    catch(Exception e)
    {
      System.out.println(e);
      String s;
      try
      {
        s = userInput("<Oooch/>");
      }
      catch(Exception exception) 
      {
        exception.printStackTrace();
      }
    }
    return retString;
  }

  public static void main(String[] args)
  {
    PCA9685 servoBoard = new PCA9685();
    int freq = 60;
    servoBoard.setPWMFreq(freq); // Set frequency in Hz
    
    // For the Parallax Futaba S148
    int servoMin = 130;   // Full speed backward
    int servoMax = 615;   // Full speed forward

    final int CONTINUOUS_SERVO_CHANNEL   = 14;
    
    int servo = CONTINUOUS_SERVO_CHANNEL;
    
    boolean keepGoing = true;
    System.out.println("Enter 'quit' to exit.");
    while (keepGoing)
    {
      String s1 = userInput("Speed (0: stop, -100: full speed backward, 100: full speed forward) ? > ");
      if ("QUIT".equalsIgnoreCase(s1))
        keepGoing = false;
      else
      {
        try
        {
          int speed = Integer.parseInt(s1);
          if (speed < -100 || speed > 100)
            System.err.println("Between -100 and 100 only");
          else
          {
            int on = 0;
            int off = (int)(servoMin + (((double)(speed + 100) / 200d) * (servoMax - servoMin)));
            System.out.println("setPWM(" + servo + ", " + on + ", " + off + ");");
            servoBoard.setPWM(servo, on, off);
            System.out.println("-------------------");
          }
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
    } 
    System.out.println("Done.");
  }
}
