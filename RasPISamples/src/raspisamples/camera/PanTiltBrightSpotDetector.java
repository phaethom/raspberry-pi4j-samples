package raspisamples.camera;

import image.util.ImageUtil;
import java.awt.Image;
import java.awt.Point;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import raspisamples.servo.StandardServo;

/**
 * Driven by the bright spot detection 
 * in a picture taken by the camera
 * 
 * 2 Servos (UP/LR)
 */
public class PanTiltBrightSpotDetector
{
  private final static int    SNAP_WIDTH  = 200;
  private final static int    SNAP_HEIGHT = 150;
  private final static String SNAPSHOT_COMMAND = "raspistill -rot 270 --width " + SNAP_WIDTH + 
                                                                    " --height " + SNAP_HEIGHT + 
                                                                    " --timeout 1 --nopreview" +
                                                                    " --output ";
  
  private static StandardServo ssUD = null, 
                               ssLR = null;
  
  private static float udAngle = 0f,
                       lrAngle = 0f;

  private final static float INCREMENT = 2f;
  private static boolean keepLooping = true;

  private final static NumberFormat NF  = new DecimalFormat("00000");
  private static String genSnapName(int idx) 
  {
    return "pix/snap_" + NF.format(idx) + ".jpg";
  }
  
  public static void main(String[] args) throws Exception
  {
    ssUD = new StandardServo(14); // 14 : Address on the board (1..15)
    ssLR = new StandardServo(15); // 15 : Address on the board (1..15)
    
    // Init/Reset
    ssUD.stop();
    ssLR.stop();
    ssUD.setAngle(udAngle);
    ssLR.setAngle(lrAngle);
    
    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      public void run()
      {
        close();
      }
    });
    
//  StandardServo.waitfor(2000);
    
    // The image/spot detection loop
    Runtime rt = Runtime.getRuntime();
    int index = 0;
    while (keepLooping)
    {
      long before = System.currentTimeMillis();
      String snapName = genSnapName(index++);
      String command = SNAPSHOT_COMMAND + snapName;
      Process snap = rt.exec(command);
      snap.waitFor(); // Sync
      long after = System.currentTimeMillis();
      System.out.println("Snapshot taken in " + Long.toString(after - before) + " ms.");
      // Detect brightest spot here
      // Analyze image here. Determine brightest color. => findSpot
      Image snapshot = ImageUtil.readImage(snapName);
      try { Thread.sleep(100L); } catch (Exception ex) {}
      Point spot = null;
      try { spot = ImageUtil.findMaxLum(snapshot); } catch (Exception ex) { ex.printStackTrace(); }
      // Drive servos here
      if (spot != null)
      {
        System.out.println("Bright Spot in " + snapName + " found at " + spot.x + "/" + spot.y + " (for " + snapshot.getWidth(null) + "x" + snapshot.getHeight(null) + ")");
        if (spot.x > (SNAP_WIDTH / 2)) // turn right
        {
          lrAngle -= INCREMENT;
          ssLR.setAngle(lrAngle);
          if ("true".equals(System.getProperty("verbose", "false")))
            System.out.println("Truning Right - LR:" + lrAngle);
        }
        else if (spot.x < (SNAP_WIDTH / 2)) // turn left
        {
          lrAngle += INCREMENT;
          ssLR.setAngle(lrAngle);
          if ("true".equals(System.getProperty("verbose", "false")))
            System.out.println("Truning Right - LR:" + lrAngle);
        }
        if (spot.y > (SNAP_HEIGHT / 2)) // look down
        {
          udAngle -= INCREMENT;
          ssUD.setAngle(udAngle);          
          if ("true".equals(System.getProperty("verbose", "false")))
            System.out.println("Looking Up - UD:" + udAngle);
        }
        else if (spot.y < (SNAP_HEIGHT / 2)) // look up
        {
          udAngle += INCREMENT;
          ssUD.setAngle(udAngle);                    
          if ("true".equals(System.getProperty("verbose", "false")))
            System.out.println("Looking Up - UD:" + udAngle);
        }
      }
//    try { Thread.sleep(500L); } catch (Exception ex) {}
    }
    System.out.println("Done looping.");
  }

  public static void close()
  {
    System.out.println("\nExiting...");
    keepLooping = false;
    StandardServo.waitfor(2000);
    // Reset to 0,0 before shutting down.
    System.out.println("Reseting servos");
    ssUD.setAngle(0f);
    ssLR.setAngle(0f);
    ssUD.stop();
    ssLR.stop();
    System.out.println("Bye");
  }
}
