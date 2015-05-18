package weatherstation.ws;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketFeeder
{
  private WebSocketClient webSocketClient = null;
  
  public WebSocketFeeder() throws Exception
  {
    String wsUri = System.getProperty("ws.uri", "ws://localhost:9876/"); 
    initWebSocketConnection(wsUri);
  }
  
  private void initWebSocketConnection(String serverURI)
  {
    try
    {
      webSocketClient = new WebSocketClient(new URI(serverURI))
        {
        @Override
        public void onOpen(ServerHandshake serverHandshake)
        {
          // TODO Implement this method
        }

        @Override
        public void onMessage(String string)
        {
          // TODO Implement this method
        }

        @Override
        public void onClose(int i, String string, boolean b)
        {
          // TODO Implement this method
        }

        @Override
        public void onError(Exception exception)
        {
          // TODO Implement this method
        }
      };
      webSocketClient.connect();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }    
  }
  
  public void pushMessage(String mess)
  {
    if ("true".equals(System.getProperty("verbose", "false")))
      System.out.println("Enqueuing [" + mess + "]");
    webSocketClient.send(mess);
  }
  
  public void shutdown()
  {
    webSocketClient.close();
  }
}

