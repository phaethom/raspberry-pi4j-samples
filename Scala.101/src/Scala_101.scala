import adafruiti2c.sensor.AdafruitBMP180

object Scala_101 {
  def main(args: Array[String]) {
    println("Hello, Scala world!")
    val bmp180 = new AdafruitBMP180();
    try
    {
      val temp  = bmp180.readTemperature()
      val press = bmp180.readPressure() / 100
      println("Temp:" + temp + "\272C, Press:" + press + " hPa")
    }
    catch
    {
      case ex: Exception => {
        println(ex.toString())
      }
    }
  }
}
