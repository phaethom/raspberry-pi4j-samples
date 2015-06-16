package breadboard.game;

/**
 * This shows how to hide the complexity of the GameMaster
 * Just implement the onButtonPressed method, and you're good.
 */
public class ReflexGameInstance implements PushButtonObserver
{
  private static long after  = 0L;

  private static ReflexGameMaster rgm = null;

  public static void main(String[] args)
  {
    ReflexGameInstance instance = new ReflexGameInstance();
    rgm = new ReflexGameMaster(instance);
    rgm.initCtx();                                   // Initialize
//  rgm.initCtx(RaspiPin.GPIO_01, RaspiPin.GPIO_02); // Can override default pins
    rgm.go();                                        // Game starts...
    rgm.freeResources();                             // Free and exit
  }

  @Override
  public void onButtonPressed()
  {
    after = System.currentTimeMillis();
    long before = rgm.getStartTime();
    if (before > 0)
    {
      System.out.println("It took you " + Long.toString(after - before) + " ms.");
      rgm.release(); // Tell it we're done.
    }
    else
    {
      System.out.println("Not yet, you idiot!");
    }
  }
}
