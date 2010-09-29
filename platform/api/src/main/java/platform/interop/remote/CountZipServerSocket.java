package platform.interop.remote;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class CountZipServerSocket extends ServerSocket
{
  public CountZipServerSocket(int port) throws IOException {
    super(port);
  }

  public Socket accept() throws IOException {
    Socket socket = new CountZipSocket();
    implAccept(socket);
    return socket;
  }
}
