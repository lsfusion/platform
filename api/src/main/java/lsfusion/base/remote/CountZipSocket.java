package lsfusion.base.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;

public class CountZipSocket extends Socket {

    private CompressedStreamObserver observer;

    private CompressedInputStream in;
    private CompressedOutputStream out;

    private CompressedOutputStream createOut() throws IOException {
        return new CompressedOutputStream(super.getOutputStream(), 2048, observer);
    }

    private CompressedInputStream createIn() throws IOException {
        return new CompressedInputStream(super.getInputStream(), 2048, observer);
    }

    public CountZipSocket(SocketImpl impl) throws SocketException {
        super(impl);
    }

    public CountZipSocket(String host, int port) throws IOException {
        super(host, port);
    }

    public void setObserver(CompressedStreamObserver observer) {
        this.observer = observer;
    }

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        if (in == null) {
            in = createIn();
        }
        return in;
    }

    @Override
    public synchronized OutputStream getOutputStream() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }
        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        if (isOutputShutdown()) {
            throw new SocketException("Socket output is shutdown");
        }

        if (out == null) {
            out = createOut();
        }
        return out;
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        if (in != null) {
            CompressedInputStream inStream = in;
            in = null;
            inStream.close();
        }
        if (out != null) {
            CompressedOutputStream outStream = out;
            out = null;
            outStream.close();
        }
    }
}

