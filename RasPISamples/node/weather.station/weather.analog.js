/*
 * @author Olivier Le Diouris
 */
var displayTWD, displayTWS, displayGUST, thermometer, 
    displayBaro, displayHum;
    
var init = function() 
{
  displayTWD      = new Direction('twdCanvas', 100, 45, 5, true);
  displayTWS      = new AnalogDisplay('twsCanvas', 100,   65,  10,  1, true, 40);
  displayGUST     = new AnalogDisplay('gustCanvas', 100,   65,  10,  1, true, 40);
  thermometer     = new Thermometer('tmpCanvas', 200);
  displayBaro     = new AnalogDisplay('baroCanvas', 100, 1040,  10,  1, true, 40, 980);
  displayHum      = new AnalogDisplay('humCanvas',  100,  100,  10,  1, true, 40);
  
  var connection;

  // if user is running mozilla then use it's built-in WebSocket
  //  window.WebSocket = window.WebSocket || window.MozWebSocket;  // TODO otherwise, fall back
  var ws = window.WebSocket || window.MozWebSocket;  // TODO otherwise, fall back

  // if browser doesn't support WebSocket, just show some notification and exit
  //  if (!window.WebSocket) 

  if (!ws) 
  {
    alert('Sorry, but your browser does not support WebSockets.'); // TODO Fallback
    return;
  }

  // open connection
  var rootUri = "ws://" + (document.location.hostname === "" ? "localhost" : document.location.hostname) + ":" +
                          (document.location.port === "" ? "9876" : document.location.port);
  console.log(rootUri);
  connection = new WebSocket(rootUri); // 'ws://localhost:9876');

  connection.onopen = function () 
  {
    console.log('Connected.')
  };

  connection.onerror = function (error) 
  {
    // just in there were some problems with connection...
    alert('Sorry, but there is some problem with your connection or the server is down.');
  };

  connection.onmessage = function (message) 
  {
//  console.log('onmessage:' + JSON.stringify(message.data));
    var data = JSON.parse(message.data);
    setValues(data);
  };

};

var changeBorder = function(b) 
{
  displayTWD.setBorder(b);
  displayTWS.setBorder(b);
  displayGUST.setBorder(b);
  displayBaro.setBorder(b);
  displayHum.setBorder(b);
};

var TOTAL_WIDTH = 1200;

var resizeDisplays = function(width)
{
  displayTWS.setDisplaySize(100 * (Math.min(width, TOTAL_WIDTH) / TOTAL_WIDTH)); 
  displayGUST.setDisplaySize(100 * (Math.min(width, TOTAL_WIDTH) / TOTAL_WIDTH)); 
  displayTWD.setDisplaySize(100 * (Math.min(width, TOTAL_WIDTH) / TOTAL_WIDTH)); 
  thermometer.setDisplaySize(200 * (Math.min(width, TOTAL_WIDTH) / TOTAL_WIDTH)); 
  displayBaro.setDisplaySize(100 * (Math.min(width, TOTAL_WIDTH) / TOTAL_WIDTH)); 
  displayHum.setDisplaySize(100 * (Math.min(width, TOTAL_WIDTH) / TOTAL_WIDTH)); 
};
  
var setValues = function(doc)
{
  try
  {
    var errMess = "";
    
    var json = doc;

    // Displays
    try
    {
      var twd = parseFloat(json.dir.toFixed(0)) % 360;
//    displayTWD.animate(twd);
      displayTWD.setValue(twd);
    }
    catch (err)
    {
      errMess += ((errMess.length > 0?"\n":"") + "Problem with TWD...");
//    displayTWD.animate(0.0);
      displayTWD.setValue(0.0);
    }

    try
    {
      var tws = parseFloat(json.speed.toFixed(2));
//    displayTWS.animate(tws);
      displayTWS.setValue(tws);
    }
    catch (err)
    {
      errMess += ((errMess.length > 0?"\n":"") + "Problem with TWS...");
//    displayTWS.animate(0.0);
      displayTWS.setValue(0.0);
    }
    try
    {
      var gust = parseFloat(json.gust.toFixed(2));
//    displayTWS.animate(tws);
      displayGUST.setValue(gust);
    }
    catch (err)
    {
      errMess += ((errMess.length > 0?"\n":"") + "Problem with TWS...");
//    displayTWS.animate(0.0);
      displayTWS.setValue(0.0);
    }
    try
    {
      var temp = parseFloat(json.temp.toFixed(1));
//    thermometer.animate(waterTemp);
      thermometer.setValue(temp);
    }
    catch (err)
    {
      errMess += ((errMess.length > 0?"\n":"") + "Problem with temperature...");
//    thermometer.animate(0.0);
      thermometer.setValue(0.0);
    }
    try
    {
      var baro = parseFloat(json.press / 100);
      if (baro != 0) {
//      displayBaro.animate(baro);
        displayBaro.setValue(baro);
      }
    }
    catch (err)
    {
//    errMess += ((errMess.length > 0?"\n":"") + "Problem with air Barometric_Pressure...");
//    displayBaro.animate(0.0);
//    displayBaro.setValue(1013.0);
    }
    try
    {
      if (json.hum !== undefined) {
        var hum = parseFloat(json.hum);
        document.getElementById('humCanvas').style.display = 'inline';
        if (hum > 0) {
  //      displayHum.animate(airTemp);
          displayHum.setValue(hum);
        }
      } else {
        document.getElementById('humCanvas').style.display = 'none';
      }
    }
    catch (err)
    {
      errMess += ((errMess.length > 0?"\n":"") + "Problem with air Relative_Humidity...");
      document.getElementById('humCanvas').style.display = 'none';
//    displayHum.animate(0.0);
      displayHum.setValue(0.0);
    }

    if (errMess !== undefined)
      document.getElementById("err-mess").innerHTML = errMess;
  }
  catch (err)
  {
    document.getElementById("err-mess").innerHTML = err;
  }
};

var lpad = function(str, pad, len)
{
  while (str.length < len)
    str = pad + str;
  return str;
};
