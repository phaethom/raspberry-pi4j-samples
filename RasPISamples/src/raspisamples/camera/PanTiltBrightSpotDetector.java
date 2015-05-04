package raspisamples.camera;

import image.util.ImageUtil;
import java.awt.Image;
import java.awt.Point;

import raspisamples.servo.StandardServo;

/**
 * Driven by the bright spot detection 
 * in a picture taken by the camera
 * 
 * 2 Servos (UP/LR)
 */
public class PanTiltBrightSpotDetector
{
  private final static String IMG_NAME    = "snap.jpg";
  private final static int    SNAP_WIDTH  = 200;
  private final static int    SNAP_HEIGHT = 150;
  private final static String SNAPSHOT_COMMAND = "raspistill -rot 180 --width " + SNAP_WIDTH + 
                                                                    " --height " + SNAP_HEIGHT + 
                                                                    " --timeout 1 --output " + IMG_NAME + 
                                                                    " --nopreview";
  
  private static StandardServo ssUD = null, 
                               ssLR = null;
  
  private static float udAngle = 0f,
                       lrAngle = 0f;

  private final static float INCREMENT = 1f;
  
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
    boolean keepLooping = true;
    Runtime rt = Runtime.getRuntime();
    while (keepLooping)
    {
      long before = System.currentTimeMillis();
      Process snap = rt.exec(SNAPSHOT_COMMAND);
      snap.waitFor(); // Sync
      long after = System.currentTimeMillis();
      System.out.println("Snapshot taken in " + Long.toString(after - before) + " ms.");
      // Detect brightest spot here
      // Analyze image here. Determine brightest color. => findSpot
      Image snapshot = ImageUtil.readImage(IMG_NAME);
      Point spot = ImageUtil.findMaxLum(snapshot);
      // Drive servos here
      if (spot != null)
      {
        if (spot.x > (SNAP_WIDTH / 2)) // turn left
        {
          udAngle -= INCREMENT;
          ssUD.setAngle(udAngle);
        }
        else if (spot.x < (SNAP_WIDTH / 2)) // turn right
        {
          udAngle += INCREMENT;
          ssUD.setAngle(udAngle);
        }
        if (spot.y > (SNAP_HEIGHT / 2)) // turn up
        {
          lrAngle += INCREMENT;
          ssLR.setAngle(lrAngle);          
        }
        else if (spot.y < (SNAP_HEIGHT / 2)) // turn down
        {
          lrAngle -= INCREMENT;
          ssLR.setAngle(lrAngle);                    
        }
      }
    }
  }

  public static void close()
  {
    System.out.println("\nExiting...");
    // Reset to 0,0 before shutting down.
    ssUD.setAngle(0f);
    ssLR.setAngle(0f);
    StandardServo.waitfor(2000);
    ssUD.stop();
    ssLR.stop();
    System.out.println("Bye");
  }
}
