package lsfusion.interop.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CountZipServerSocket extends ServerSocket {
    public CountZipServerSocket(int port) throws IOException {
        super(port);
    }

    public Socket accept() throws IOException {
        Socket socket = new CountZipSocket();
        implAccept(socket);
        return socket;
    }
}
