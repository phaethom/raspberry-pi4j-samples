package adafruiti2c.adc;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

import java.util.Set;

public class AdafruitADS1x15
{
  private static boolean verbose = ("true".equals(System.getProperty("verbose", "false")));

  // IC Identifiers
  public enum ICType
  {
    IC_ADS1015,
    IC_ADS1115
  }
  
  // Available channels
  public enum Channels
  {
    CHANNEL_0,
    CHANNEL_1,
    CHANNEL_2,
    CHANNEL_3
  }
  
  private final static int ADS1x15_ADDRESS = 0x48;
  // Pointer Register
  private final static int ADS1015_REG_POINTER_MASK        = 0x03;
  private final static int ADS1015_REG_POINTER_CONVERT     = 0x00;
  private final static int ADS1015_REG_POINTER_CONFIG      = 0x01;
  private final static int ADS1015_REG_POINTER_LOWTHRESH   = 0x02;
  private final static int ADS1015_REG_POINTER_HITHRESH    = 0x03;

  // Config Register
  private final static int ADS1015_REG_CONFIG_OS_MASK      = 0x8000;
  private final static int ADS1015_REG_CONFIG_OS_SINGLE    = 0x8000;  // Write: Set to start a single-conversion
  private final static int ADS1015_REG_CONFIG_OS_BUSY      = 0x0000;  // Read: Bit = 0 when conversion is in progress
  private final static int ADS1015_REG_CONFIG_OS_NOTBUSY   = 0x8000;  // Read: Bit = 1 when device is not performing a conversion

  private final static int ADS1015_REG_CONFIG_MUX_MASK     = 0x7000;
  private final static int ADS1015_REG_CONFIG_MUX_DIFF_0_1 = 0x0000;  // Differential P = AIN0, N = AIN1 (default)
  private final static int ADS1015_REG_CONFIG_MUX_DIFF_0_3 = 0x1000;  // Differential P = AIN0, N = AIN3
  private final static int ADS1015_REG_CONFIG_MUX_DIFF_1_3 = 0x2000;  // Differential P = AIN1, N = AIN3
  private final static int ADS1015_REG_CONFIG_MUX_DIFF_2_3 = 0x3000;  // Differential P = AIN2, N = AIN3
  private final static int ADS1015_REG_CONFIG_MUX_SINGLE_0 = 0x4000;  // Single-ended AIN0
  private final static int ADS1015_REG_CONFIG_MUX_SINGLE_1 = 0x5000;  // Single-ended AIN1
  private final static int ADS1015_REG_CONFIG_MUX_SINGLE_2 = 0x6000;  // Single-ended AIN2
  private final static int ADS1015_REG_CONFIG_MUX_SINGLE_3 = 0x7000;  // Single-ended AIN3

  private final static int ADS1015_REG_CONFIG_PGA_MASK     = 0x0E00;
  private final static int ADS1015_REG_CONFIG_PGA_6_144V   = 0x0000;  // +/-6.144V range
  private final static int ADS1015_REG_CONFIG_PGA_4_096V   = 0x0200;  // +/-4.096V range
  private final static int ADS1015_REG_CONFIG_PGA_2_048V   = 0x0400;  // +/-2.048V range (default)
  private final static int ADS1015_REG_CONFIG_PGA_1_024V   = 0x0600;  // +/-1.024V range
  private final static int ADS1015_REG_CONFIG_PGA_0_512V   = 0x0800;  // +/-0.512V range
  private final static int ADS1015_REG_CONFIG_PGA_0_256V   = 0x0A00;  // +/-0.256V range

  private final static int ADS1015_REG_CONFIG_MODE_MASK    = 0x0100;
  private final static int ADS1015_REG_CONFIG_MODE_CONTIN  = 0x0000;  // Continuous conversion mode
  private final static int ADS1015_REG_CONFIG_MODE_SINGLE  = 0x0100;  // Power-down single-shot mode (default)

  private final static int ADS1015_REG_CONFIG_DR_MASK      = 0x00E0;  
  private final static int ADS1015_REG_CONFIG_DR_128SPS    = 0x0000;  // 128 samples per second
  private final static int ADS1015_REG_CONFIG_DR_250SPS    = 0x0020;  // 250 samples per second
  private final static int ADS1015_REG_CONFIG_DR_490SPS    = 0x0040;  // 490 samples per second
  private final static int ADS1015_REG_CONFIG_DR_920SPS    = 0x0060;  // 920 samples per second
  private final static int ADS1015_REG_CONFIG_DR_1600SPS   = 0x0080;  // 1600 samples per second (default)
  private final static int ADS1015_REG_CONFIG_DR_2400SPS   = 0x00A0;  // 2400 samples per second
  private final static int ADS1015_REG_CONFIG_DR_3300SPS   = 0x00C0;  // 3300 samples per second (also 0x00E0)

  private final static int ADS1115_REG_CONFIG_DR_8SPS      = 0x0000;  // 8 samples per second
  private final static int ADS1115_REG_CONFIG_DR_16SPS     = 0x0020;  // 16 samples per second
  private final static int ADS1115_REG_CONFIG_DR_32SPS     = 0x0040;  // 32 samples per second
  private final static int ADS1115_REG_CONFIG_DR_64SPS     = 0x0060;  // 64 samples per second
  private final static int ADS1115_REG_CONFIG_DR_128SPS    = 0x0080;  // 128 samples per second
  private final static int ADS1115_REG_CONFIG_DR_250SPS    = 0x00A0;  // 250 samples per second (default)
  private final static int ADS1115_REG_CONFIG_DR_475SPS    = 0x00C0;  // 475 samples per second
  private final static int ADS1115_REG_CONFIG_DR_860SPS    = 0x00E0;  // 860 samples per second

  private final static int ADS1015_REG_CONFIG_CMODE_MASK   = 0x0010;
  private final static int ADS1015_REG_CONFIG_CMODE_TRAD   = 0x0000;  // Traditional comparator with hysteresis (default)
  private final static int ADS1015_REG_CONFIG_CMODE_WINDOW = 0x0010;  // Window comparator

  private final static int ADS1015_REG_CONFIG_CPOL_MASK    = 0x0008;
  private final static int ADS1015_REG_CONFIG_CPOL_ACTVLOW = 0x0000;  // ALERT/RDY pin is low when active (default)
  private final static int ADS1015_REG_CONFIG_CPOL_ACTVHI  = 0x0008;  // ALERT/RDY pin is high when active

  private final static int ADS1015_REG_CONFIG_CLAT_MASK    = 0x0004;  // Determines if ALERT/RDY pin latches once asserted
  private final static int ADS1015_REG_CONFIG_CLAT_NONLAT  = 0x0000;  // Non-latching comparator (default)
  private final static int ADS1015_REG_CONFIG_CLAT_LATCH   = 0x0004;  // Latching comparator

  private final static int ADS1015_REG_CONFIG_CQUE_MASK    = 0x0003;
  private final static int ADS1015_REG_CONFIG_CQUE_1CONV   = 0x0000;  // Assert ALERT/RDY after one conversions
  private final static int ADS1015_REG_CONFIG_CQUE_2CONV   = 0x0001;  // Assert ALERT/RDY after two conversions
  private final static int ADS1015_REG_CONFIG_CQUE_4CONV   = 0x0002;  // Assert ALERT/RDY after four conversions
  private final static int ADS1015_REG_CONFIG_CQUE_NONE    = 0x0003;  // Disable the comparator and put ALERT/RDY in high state (default)
  
  public enum spsADS1115 
  {
    ADS1115_REG_CONFIG_DR_8SPS(8),
    ADS1115_REG_CONFIG_DR_16SPS(16),
    ADS1115_REG_CONFIG_DR_32SPS(32),
    ADS1115_REG_CONFIG_DR_64SPS(64),
    ADS1115_REG_CONFIG_DR_128SPS(128),
    ADS1115_REG_CONFIG_DR_250SPS(250),
    ADS1115_REG_CONFIG_DR_475SPS(475),
    ADS1115_REG_CONFIG_DR_860SPS(860);

    private final int value;
    spsADS1115(int value) { this.value = value; }    
    public int value() { return this.value; }
    
    public static int setDefault(int val, int def)
    {
      int ret = def;
      boolean found = false;
      for (spsADS1115 one : values())
      {
        if (one.value() == val)
        {
          ret = val;
          found = true;
          break;
        }
      }
      if (!found)
      {
        if (verbose)
          System.out.println("Value [" + val + "] not found, defaulting to [" + def + "]");
        // Check if default value is in the list
        found = false;
        for (spsADS1115 one : values())
        {
          if (one.value() == def)
          {
            ret = val;
            found = true;
            break;
          }
        }
        if (!found)
        {
          System.out.println("Just FYI... default value is not in the enum...");
        }
      }
      return ret;
    }
  }    
  
  public enum spsADS1015
  {
    ADS1015_REG_CONFIG_DR_128SPS(128),
    ADS1015_REG_CONFIG_DR_250SPS(250),
    ADS1015_REG_CONFIG_DR_490SPS(490),
    ADS1015_REG_CONFIG_DR_920SPS(920),
    ADS1015_REG_CONFIG_DR_1600SPS(1600),
    ADS1015_REG_CONFIG_DR_2400SPS(2400),
    ADS1015_REG_CONFIG_DR_3300SPS(3300);

    private final int value;
    spsADS1015(int value) { this.value = value; }    
    public int value() { return this.value; }
    
    public static int setDefault(int val, int def)
    {
      int ret = def;
      boolean found = false;
      for (spsADS1015 one : values())
      {
        if (one.value() == val)
        {
          ret = val;
          found = true;
          break;
        }
      }
      if (!found)
      {
        if (verbose)
          System.out.println("Value [" + val + "] not found, defaulting to [" + def + "]");
        // Check if default value is in the list
        found = false;
        for (spsADS1015 one : values())
        {
          if (one.value() == def)
          {
            ret = val;
            found = true;
            break;
          }
        }
        if (!found)
        {
          System.out.println("Just FYI... default value is not in the enum...");
        }
      }
      return ret;
    }
  }
  
  // Dictionary with the programmable gains
  public enum pgaADS1x15 
  {
    ADS1015_REG_CONFIG_PGA_6_144V(6144),
    ADS1015_REG_CONFIG_PGA_4_096V(4096),
    ADS1015_REG_CONFIG_PGA_2_048V(2048),
    ADS1015_REG_CONFIG_PGA_1_024V(1024),
    ADS1015_REG_CONFIG_PGA_0_512V(512),
    ADS1015_REG_CONFIG_PGA_0_256V(256);

    private final int value;
    pgaADS1x15(int value) { this.value = value; }    
    public int value() { return this.value; }

    public static int setDefault(int val, int def)
    {
      int ret = def;
      boolean found = false;
      for (pgaADS1x15 one : values())
      {
        if (one.value() == val)
        {
          ret = val;
          found = true;
          break;
        }
      }
      if (!found)
      {
        if (verbose)
          System.out.println("Value [" + val + "] not found, defaulting to [" + def + "]");
        // Check if default value is in the list
        found = false;
        for (pgaADS1x15 one : values())
        {
          if (one.value() == def)
          {
            ret = val;
            found = true;
            break;
          }
        }
        if (!found)
        {
          System.out.println("Just FYI... default value is not in the enum...");
        }
      }
      return ret;
    }
  }    
  
  private I2CBus    bus;
  private I2CDevice adc;
  
  private ICType adcType;
  private int pga;
    
  public AdafruitADS1x15()
  {
    this(ICType.IC_ADS1015);
  }
  public AdafruitADS1x15(int address)
  {
    this(ICType.IC_ADS1015, address);
  }
  public AdafruitADS1x15(ICType icType)
  {
    this(icType, ADS1x15_ADDRESS);
  }
  public AdafruitADS1x15(ICType icType, int address)
  {
    this.adcType = icType;
    
    try
    {
      // Get I2C bus
      bus = I2CFactory.getInstance(I2CBus.BUS_1); // Depends onthe RasPI version
      if (verbose)
        System.out.println("Connected to bus. OK.");

      // Get the device itself
      adc = bus.getDevice(address); 
      if (verbose)
        System.out.println("Connected to device. OK.");

      // Set pga value, so that getLastConversionResult() can use it,
      // any function that accepts a pga value must update this.
      this.pga = 6144;

    }
    catch (IOException e)
    {
      System.err.println(e.getMessage());
    }
  }
  
  public int readADCSingleEnded()
  {
    return readADCSingleEnded(Channels.CHANNEL_0);
  }
  
  public int readADCSingleEnded(Channels channel)
  {
    return readADCSingleEnded(channel, 6144, 250);
  }

  /**
   * Gets a single-ended ADC reading from the specified channel in mV. 
   * The sample rate for this mode (single-shot) can be used to lower the noise 
   * (low sps) or to lower the power consumption (high sps) by duty cycling, 
   * see datasheet page 14 for more info. 
   * The pga must be given in mV, see page 13 for the supported values.
   *
   * @param channel 0-3
   * @param pga
   * @param sps Samples per second
   * @return
   */
  public int readADCSingleEnded(Channels channel, int pga, int sps)
  {
    // Disable comparator, Non-latching, Alert/Rdy active low
    // traditional comparator, single-shot mode
    int config = ADS1015_REG_CONFIG_CQUE_NONE    | 
                 ADS1015_REG_CONFIG_CLAT_NONLAT  | 
                 ADS1015_REG_CONFIG_CPOL_ACTVLOW | 
                 ADS1015_REG_CONFIG_CMODE_TRAD   | 
                 ADS1015_REG_CONFIG_MODE_SINGLE;    

    // Set sample per seconds, defaults to 250sps
    // If sps is in the dictionary (defined in init) it returns the value of the constant
    // othewise it returns the value for 250sps. This saves a lot of if/elif/else code!
    if (this.adcType.equals(ICType.IC_ADS1015))
    {
      int _sps = spsADS1015.setDefault(sps, ADS1015_REG_CONFIG_DR_1600SPS);
      config |= _sps;
    }
    else
    {
      int _sps = spsADS1015.setDefault(sps, ADS1115_REG_CONFIG_DR_250SPS);
      config |= _sps;
    }
    // Set PGA/voltage range, defaults to +-6.144V
    int _pga = pgaADS1x15.setDefault(pga, ADS1015_REG_CONFIG_PGA_6_144V);
    config |= _pga;

    this.pga = _pga;

    // Set the channel to be converted
    if (channel == Channels.CHANNEL_3)
      config |= ADS1015_REG_CONFIG_MUX_SINGLE_3;
    else if (channel == Channels.CHANNEL_2)
      config |= ADS1015_REG_CONFIG_MUX_SINGLE_2;
    else if (channel == Channels.CHANNEL_1)
      config |= ADS1015_REG_CONFIG_MUX_SINGLE_1;
    else
      config |= ADS1015_REG_CONFIG_MUX_SINGLE_0;

    // Set 'start single-conversion' bit
    config |= ADS1015_REG_CONFIG_OS_SINGLE;

    // Write config register to the ADC
    byte[] bytes = { (byte)((config >> 8) & 0xFF), (byte)(config & 0xFF) };
    try
    {
      adc.write(ADS1015_REG_POINTER_CONFIG, bytes, 0, 2);
    }
    catch (IOException ioe)
    {
      System.err.println("Ooops");
      ioe.printStackTrace();
    }

    // Wait for the ADC conversion to complete
    // The minimum delay depends on the sps: delay >= 1/sps
    // We add 0.1ms to be sure
    long delay = (long)(1000 / ((sps * 1000) + 0.1));
    try { Thread.sleep(delay); } catch (InterruptedException ie) {}

    // Read the conversion results
    byte[] result = new byte[2];
    try
    {
      int ret = adc.read(ADS1015_REG_POINTER_CONVERT, result, 0, 2);
    }
    catch (IOException ioe)
    {
      System.err.println("Ooops - 2");
      ioe.printStackTrace();
    }
    
    int returnVal = 0;
    if (this.adcType == ICType.IC_ADS1015)
    {
      // Shift right 4 bits for the 12-bit ADS1015 and convert to mV
      returnVal = ( ((result[0] << 8) | (result[1] & 0xFF)) >> 4);
      returnVal = (int)(returnVal * pga / 2048.0);
    }
    else
    {
      // Return a mV value for the ADS1115
      // (Take signed values into account as well)
      int val = (result[0] << 8) | (result[1]);
      if (val > 0x7FFF)
        returnVal = (int)((val - 0xFFFF) * pga / 32768.0);
      else
      {
        returnVal = ((result[0] << 8) | (result[1]));
        returnVal = (int)(returnVal * pga / 32768.0);
      }
    }
    return returnVal;
  }
  
  public int readADCDifferential()
  {
    return readADCDifferential(Channels.CHANNEL_0, Channels.CHANNEL_1, 6144, 250);
  }
  /**
   * Gets a differential ADC reading from channels chP and chN in mV. 
   * The sample rate for this mode (single-shot) can be used to lower the noise 
   * (low sps) or to lower the power consumption (high sps) by duty cycling, 
   * see data sheet page 14 for more info. 
   * The pga must be given in mV, see page 13 for the supported values.
   */
  public int readADCDifferential(Channels chP, Channels chN, int pga, int sps)
  {
    // Disable comparator, Non-latching, Alert/Rdy active low
    // traditional comparator, single-shot mode    
    int config = ADS1015_REG_CONFIG_CQUE_NONE    | 
                 ADS1015_REG_CONFIG_CLAT_NONLAT  | 
                 ADS1015_REG_CONFIG_CPOL_ACTVLOW | 
                 ADS1015_REG_CONFIG_CMODE_TRAD   | 
                 ADS1015_REG_CONFIG_MODE_SINGLE;  
    
    // Set channels
    if ((chP == Channels.CHANNEL_0) && (chN == Channels.CHANNEL_1))     // 0 1
      config |= ADS1015_REG_CONFIG_MUX_DIFF_0_1;
    else if ((chP == Channels.CHANNEL_0) & (chN == Channels.CHANNEL_3)) // 0 3
      config |= ADS1015_REG_CONFIG_MUX_DIFF_0_3;
    else if ((chP == Channels.CHANNEL_2) & (chN == Channels.CHANNEL_3)) // 2 3 
      config |= ADS1015_REG_CONFIG_MUX_DIFF_2_3;
    else if ((chP == Channels.CHANNEL_1) & (chN == Channels.CHANNEL_3)) // 1 3 
      config |= ADS1015_REG_CONFIG_MUX_DIFF_1_3  ;
    else
    {
      if (verbose)
      {
        System.out.printf("ADS1x15: Invalid channels specified: %d, %d\n", chP, chN);
        return -1;
      }
    }     
    // Set sample per seconds, defaults to 250sps
    // If sps is in the dictionary (defined in init()) it returns the value of the constant
    // othewise it returns the value for 250sps. This saves a lot of if/elif/else code!
    if (this.adcType == ICType.IC_ADS1015)
      config |= spsADS1015.setDefault(sps, ADS1015_REG_CONFIG_DR_1600SPS);
    else
      config |= spsADS1115.setDefault(sps, ADS1115_REG_CONFIG_DR_250SPS);

    // Set PGA/voltage range, defaults to +-6.144V
    this.pga = pgaADS1x15.setDefault(pga, ADS1015_REG_CONFIG_PGA_6_144V);
    config |= this.pga;

    // Set 'start single-conversion' bit
    config |= ADS1015_REG_CONFIG_OS_SINGLE;

    // Write config register to the ADC
    byte[] bytes = { (byte)((config >> 8) & 0xFF), (byte)(config & 0xFF) };
    
    try
    {	
      adc.write(ADS1015_REG_POINTER_CONFIG, bytes, 0, 2);
    }
    catch(IOException ioe)
    {
      ioe.printStackTrace();
    }

    // Wait for the ADC conversion to complete
    // The minimum delay depends on the sps: delay >= 1/sps
    // We add 0.1ms to be sure
    long delay = (long)(1000 / ((sps * 1000) + 0.1));
    try { Thread.sleep(delay); } catch (InterruptedException ie) {}


    // Read the conversion results
    byte[] result = new byte[2];
    try
    {
      int ret = adc.read(ADS1015_REG_POINTER_CONVERT, result, 0, 2);
    }
    catch (IOException ioe)
    {
      System.err.println("Ooops - 2");
      ioe.printStackTrace();
    }
    
    int returnVal = 0;
    if (this.adcType == ICType.IC_ADS1015)
    {
      // Shift right 4 bits for the 12-bit ADS1015 and convert to mV
      returnVal = ( ((result[0] << 8) | (result[1] & 0xFF)) >> 4);
      returnVal = (int)(returnVal * pga / 2048.0);
    }
    else
    {
      // Return a mV value for the ADS1115
      // (Take signed values into account as well)
      int val = (result[0] << 8) | (result[1]);
      if (val > 0x7FFF)
        returnVal = (int)((val - 0xFFFF) * pga / 32768.0);
      else
      {
        returnVal = ((result[0] << 8) | (result[1]));
        returnVal = (int)(returnVal * pga / 32768.0);
      }
    }
    return returnVal;
  }
  
  public int readADCDifferential01()
  {
    return readADCDifferential01(6144, 250);
  }
  /**
   * Gets a differential ADC reading from channels 0 and 1 in mV
   * The sample rate for this mode (single-shot) can be used to lower the noise 
   * (low sps) or to lower the power consumption (high sps) by duty cycling, 
   * see data sheet page 14 for more info. 
   * The pga must be given in mV, see page 13 for the supported values.
   */
  public int readADCDifferential01(int pga, int sps)
  {
    return readADCDifferential(Channels.CHANNEL_0, Channels.CHANNEL_1, pga, sps);
  } 
  
  public int readADCDifferential03()
  {
    return readADCDifferential03(6144, 250);
  }
  /**
   * Gets a differential ADC reading from channels 0 and 3 in mV 
   * The sample rate for this mode (single-shot) can be used to lower the noise 
   * (low sps) or to lower the power consumption (high sps) by duty cycling, 
   * see data sheet page 14 for more info. 
   * The pga must be given in mV, see page 13 for the supported values.
   */ 
  public int readADCDifferential03(int pga, int sps)
  {
    return readADCDifferential(Channels.CHANNEL_0, Channels.CHANNEL_3, pga, sps);
  }   
  
  public int readADCDifferential13()
  {
    return readADCDifferential13(6144, 250);
  }
  /**
   * Gets a differential ADC reading from channels 1 and 3 in mV 
   * The sample rate for this mode (single-shot) can be used to lower the noise 
   * (low sps) or to lower the power consumption (high sps) by duty cycling, 
   * see data sheet page 14 for more info. 
   * The pga must be given in mV, see page 13 for the supported values.
   */
  public int readADCDifferential13(int pga, int sps)
  {
    return readADCDifferential(Channels.CHANNEL_1, Channels.CHANNEL_3, pga, sps);
  }

  public int readADCDifferential23()
  {
    return readADCDifferential23(6144, 250);
  }

  /**
   * Gets a differential ADC reading from channels 2 and 3 in mV 
   * The sample rate for this mode (single-shot) can be used to lower the noise 
   * (low sps) or to lower the power consumption (high sps) by duty cycling, 
   * see data sheet page 14 for more info. 
   * The pga must be given in mV, see page 13 for the supported values.
   */
  public int readADCDifferential23(int pga, int sps)
  {
    return readADCDifferential(Channels.CHANNEL_2, Channels.CHANNEL_3, pga, sps);
  }  
}
