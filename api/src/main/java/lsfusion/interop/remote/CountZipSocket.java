package lsfusion.interop.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;

public class CountZipSocket extends Socket {

    private CompressedStreamObserver observer;

// todo: расскоментить для перехода на старый механизм архивации данных, в перспективе - удалить окончательно...
//    private CompressedBlockInputStream_Obsolete in;
//    private CompressedBlockOutputStream_Obsolete out;
//
//    private CompressedBlockOutputStream_Obsolete createOut() throws IOException {
//        return new CompressedBlockOutputStream_Obsolete(super.getOutputStream(), 1 << 20, observer);
//    }
//
//    private CompressedBlockInputStream_Obsolete createIn() throws IOException {
//        return new CompressedBlockInputStream_Obsolete(super.getInputStream(), observer);
//    }

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

    public CountZipSocket(String host, int port)
            throws IOException {
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

