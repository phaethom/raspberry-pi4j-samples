/*
 * Spot GRIB Request result parser
 * By OlivSoft
 * olivier@lediouris.net

 Sample data:
  [{
        "time": "2015-07-05 23:58:41",
        "wdir": 0,
        "gust": 0.00,
        "ws": 0.00,
        "rain": 0.000,
        "press": 1012.3800000,
        "atemp": 22.800,
        "hum": 72.499,
        "cpu": 33.600
    }, {
        "time": "2015-07-06 00:08:40",
        "wdir": 270,
        "gust": 7.95,
        "ws": 4.74,
        "rain": 0.000,
        "press": 1012.4200000,
        "atemp": 22.700,
        "hum": 73.781,
        "cpu": 32.000
    }, {...
      ]
 */
 
var SpotParser =
{
  nmeaData : [],
  position : {},

  /*
    data look like
     [{
        "time": "2015-07-05 23:58:41",
        "wdir": 0,
        "gust": 0.00,
        "ws": 0.00,
        "rain": 0.000,
        "press": 1012.3800000,
        "atemp": 22.800,
        "hum": 72.499,
        "cpu": 33.600
    }, {..} ]
  */
  
  parse : function(spotContent, cb, cb2)
  {
    SpotParser.nmeaData  = [];
    var linkList = "";
    //                           2015-07-05 23:58:41
    var regExp     = new RegExp("(\\d{4})-(\\d{2})-(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2})");  
    
    for (var i=0; i<spotContent.length; i++)
    {
      var date  = spotContent[i].time;
      var d = null;
      var matches = regExp.exec(date);
      if (matches !== null) {
        var y  = matches[1];
        var mo = matches[2];
        var d  = matches[3];
        var h  = matches[4];
        var mi = matches[5];
        var s  = matches[6];
        d = new Date(y, mo - 1, d, h, mi, s, 0);
      }
      var prmsl = spotContent[i].press;
      var tws   = spotContent[i].ws;
      var twd   = spotContent[i].wdir;
      var rain  = spotContent[i].rain;
      var temp  = spotContent[i].atemp;
      var hum   = spotContent[i].hum;
      var cpu   = spotContent[i].cpu;
      
//      console.info("Line:" + date + ":" + tws);
      SpotParser.nmeaData.push(new NMEAData(d, prmsl, tws, twd, rain, temp, hum, cpu));
    }    
  }
};

var NMEAData = function(date, prmsl, tws, twd, rain, atemp, hum, cpu)
{
  var nmeaDate = date;
  var nmeaPrmsl = prmsl;
  var nmeaTws = tws;
  var nmeaTwd = twd;
  var nmeaRain = rain;
  var nmeaTemp = atemp;
  var nmeaHum = hum;
  var nmeaCpu = cpu;
  
  this.getNMEADate = function()
  { return nmeaDate; };
  
  this.getNMEAPrmsl = function()
  { return nmeaPrmsl; };
  
  this.getNMEATws = function()
  { return nmeaTws; };
  
  this.getNMEATwd = function()
  { return nmeaTwd; };
  
  this.getNMEARain = function()
  { return nmeaRain; };
  
  this.getNMEATemp = function()
  { return nmeaTemp; };
  
  this.getNMEAHum = function()
  { return nmeaHum; };
  
  this.getNMEACpu = function()
  { return nmeaCpu; };
};
