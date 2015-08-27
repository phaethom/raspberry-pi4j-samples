package fona.arduino.sample;

import com.pi4j.io.serial.SerialPortException;

import fona.arduino.FONAClient;
import fona.arduino.ReadWriteFONA;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SampleClient
  implements FONAClient
{
  public SampleClient()
  {
    super();
  }

  @Override
  public void genericSuccess(String mess)
  {
    System.out.println("Generic success:" + mess);
  }

  @Override
  public void genericFailure(String mess)
  {
    System.out.println("Generic failure:" + mess);
  }

  @Override
  public void batteryState(String mess)
  {
    System.out.println(mess);
  }

  @Override
  public void adcState(String mess)
  {
    System.out.println(mess);
  }

  @Override
  public void ccidState(String mess)
  {
    System.out.println(mess);
  }

  @Override
  public void rssiState(String mess)
  {
    System.out.println(mess);
  }

  @Override
  public void networkState(String mess)
  {
    System.out.println(mess);
  }

  @Override
  public void numberOfMessages(int nb)
  {
    System.out.println("# of messages:" + nb);
  }

  @Override
  public void message(ReadWriteFONA.SMS sms)
  {
    System.out.println("Message # " + sms.getNum() +  ", from " +    
                                      sms.getFrom() + ", " +         
                                      sms.getLen() +  " char(s), " + 
                                      sms.getContent());
  }
  
  public static void main(String args[]) throws InterruptedException
  {      
    SampleClient client = new SampleClient();
    final ReadWriteFONA fona = new ReadWriteFONA(client);
    fona.openSerialInput();
    fona.startListening();

    try
    {
      System.out.println("Hit 'Q' to quit.");
      System.out.println("Hit [return] when ready to start.");
      userInput("");

      final Thread me = Thread.currentThread();
      Thread userInputThread = new Thread()
        {
          public void run()
          {
            displayMenu();
            boolean loop = true;
            while (loop)
            {
              String userInput = "";
              userInput = userInput("So? > ");
              if ("Q".equalsIgnoreCase(userInput))
                loop = false;
              else
              { 
            //  channel.sendSerial(userInput); // Private
                if ("?".equals(userInput))
                  displayMenu();
                else if ("b".equals(userInput))
                  fona.requestBatteryState();
                else if ("a".equals(userInput))
                  fona.requestADC();
                else if ("C".equals(userInput))
                  fona.requestSIMCardNumber();
                else if ("i".equals(userInput))
                  fona.requestRSSI();
                else if ("n".equals(userInput))
                  fona.requestNetworkStatus();
                else if ("N".equals(userInput))
                  fona.requestNumberOfMessage();
                else if ("r".equals(userInput))
                {
                  String _smsn = userInput("Mess #:");
                  fona.readMessNum(Integer.parseInt(_smsn));
                }
                else if ("d".equals(userInput))
                {
                  String _smsn = userInput("Mess #:");
                  fona.deleteMessNum(Integer.parseInt(_smsn));
                }
                else if ("s".equals(userInput))
                {
                  String to      = userInput("Send to > ");
                  String payload = userInput("Message content > ");
                  fona.sendMess(to, payload);
                }
              }
            }
            synchronized (me)
            {
              me.notify();
            }
          }
        };
      userInputThread.start();
      
      synchronized (me)
      {
        me.wait();
      }
      System.out.println("Bye!");
      fona.closeChannel();
    }
    catch (SerialPortException ex)
    {
      System.out.println(" ==>> Serial Setup failed : " + ex.getMessage());
      return;
    }
    System.exit(0);
  }
  
  private static void displayMenu()
  {
    System.out.println("[?] Display menu");
    System.out.println("[Q] to quit");
    System.out.println("[a] ADC");
    System.out.println("[b] Battery");
  }
  
  private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

  private static String userInput(String prompt)
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
}
