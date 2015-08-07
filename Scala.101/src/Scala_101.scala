import i2c.sensor.{HTU21DF, BMP180}
import com.pi4j.system.SystemInfo

object Scala_101 {
  def main(args: Array[String]) {
    println("Hello, Scala world!")
    val bmp180  = new BMP180
    val htu21df = new HTU21DF
    try
    {
      val temp  = bmp180.readTemperature
      val press = bmp180.readPressure / 100
      println("CPU Temperature   :  " + SystemInfo.getCpuTemperature + "\u00baC")
      println("Temp:" + temp + "\u00baC, Press:" + press + " hPa")
    }
    catch
    {
      case ex: Exception => {
        println(ex.toString)
      }
    }
    try
    {
      htu21df.begin
      val hum = htu21df.readHumidity
      htu21df.close
      println("Humidity:" + hum + " %")
    }
    catch
    {
      case ex: Exception => {
        println(ex.toString)
      }
    }
  }
}
