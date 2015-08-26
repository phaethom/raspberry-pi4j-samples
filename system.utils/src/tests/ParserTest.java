package tests;

import java.lang.String;
import java.lang.reflect.Method;

public class ParserTest
{

  private static Method GENERIC_FAILURE_PARSER;
  private static Method GENERIC_SUCCESS_PARSER;
  static {
    try { GENERIC_FAILURE_PARSER = ParserTest.class.getMethod("genericFailureParser", String.class); } catch (Exception ex) { ex.printStackTrace(); }
    try { GENERIC_SUCCESS_PARSER = ParserTest.class.getMethod("genericSuccessParser", String.class); } catch (Exception ex) { ex.printStackTrace(); }
  }

  public enum ArduinoMessagePrefix
  {
    FONA_OK       (">> FONA READY", "Good to go", GENERIC_SUCCESS_PARSER),
    INCOMING_MESS ("+CMTI:",        "Incoming message", null),
    BAT_OK        (">> BAT OK",     "Read Battery", GENERIC_SUCCESS_PARSER),
    BAT_FAILED    (">> BAT FAILED", "Read Battery failed", GENERIC_FAILURE_PARSER);

    private final String prefix;
    private final String meaning;
    private final Method parser;
    ArduinoMessagePrefix(String prefix, String meaning, Method parser)
    {
      this.prefix = prefix;
      this.meaning = meaning;
      this.parser = parser;
    }

    public String prefix()  { return this.prefix; }
    public String meaning() { return this.meaning; }
    public Method parser()  { return this.parser; }
  }

  public static void main(String[] args) throws Exception
  {
    takeAction(">> BAT FAILED, c'est tout pete!");
    takeAction(">> FONA READY");
    takeAction(">> BAT OK");
  }

  private static void takeAction(String mess) throws Exception
  {
    ArduinoMessagePrefix amp = findCommand(mess);
    if (amp != null)
    {
      String meaning = amp.meaning();
      Method parser = amp.parser();
      if (parser != null)
      {
        parser.invoke(ParserTest.class, mess);
      }
    }
    else
      System.out.println("Command [" + mess + "] unknown.");
  }

  private static ArduinoMessagePrefix findCommand(String message)
  {
    ArduinoMessagePrefix ret = null;
    for (ArduinoMessagePrefix amp : ArduinoMessagePrefix.values())
    {
      if (message.startsWith(amp.prefix()))
      {
        ret = amp;
        break;
      }
    }
    return ret;
  }

  public static void genericSuccessParser(String message)
  {
    System.out.println("Generic success:" + message);
  }
  public static void genericFailureParser(String message)
  {
    System.out.println("Generic failure:" + message);
  }
}