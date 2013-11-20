package lsfusion.interop.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;

public class CountZipSocket extends Socket {
    private CompressedBlockInputStream in;
    private CompressedBlockOutputStream out;
    private ISocketTrafficSum observer;

    public CountZipSocket(SocketImpl impl) throws SocketException {
        super(impl);
    }

    public CountZipSocket(String host, int port)
            throws IOException {
        super(host, port);
    }

    public void setObserver(ISocketTrafficSum observer) {
        this.observer = observer;
    }

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        if (in == null) {
            in = new CompressedBlockInputStream(super.getInputStream());
            if (observer != null) {
                in.setObserver(observer);
            }
        }
        return in;
    }

    @Override
    public synchronized OutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new CompressedBlockOutputStream(super.getOutputStream(), 1 << 20);
            if (observer != null) {
                out.setObserver(observer);
            }
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

