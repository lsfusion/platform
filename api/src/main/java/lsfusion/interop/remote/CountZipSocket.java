package lsfusion.interop.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;

public class CountZipSocket extends Socket {

    private CompressedStreamObserver observer;

    private CompressedInputStream_jre6 in;
    private CompressedOutputStream_jre6 out;

    private CompressedOutputStream_jre6 createOut() throws IOException {
        return new CompressedOutputStream_jre6(super.getOutputStream(), 8192, observer);
    }

    private CompressedInputStream_jre6 createIn() throws IOException {
        return new CompressedInputStream_jre6(super.getInputStream(), observer);
    }

// TODO: использовать простой вариант стримов, если полностью перейдём на JDK 7+
//    private CompressedInputStream in;
//    private CompressedOutputStream out;
//
//    private CompressedOutputStream createOut() throws IOException {
//        return new CompressedOutputStream(super.getOutputStream(), 2048, observer);
//    }
//
//    private CompressedInputStream createIn() throws IOException {
//        return new CompressedInputStream(super.getInputStream(), 2048, observer);
//    }

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

    public synchronized void closeIfHung() {
        if (in != null && in.hangs) {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        if (in != null) {
            in.close();
            in = null;
        }
        if (out != null) {
            out.close();
            out = null;
        }
    }
}

