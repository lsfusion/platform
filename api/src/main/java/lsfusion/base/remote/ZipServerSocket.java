package lsfusion.base.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ZipServerSocket extends ServerSocket {
    public ZipServerSocket(int port) throws IOException {
        super(port, 500);
    }

    public Socket accept() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isBound())
            throw new SocketException("Socket is not bound yet");

        Socket socket = new ZipSocket(null);
//        Socket socket = new Socket();
        implAccept(socket);
        return socket;
    }
}
