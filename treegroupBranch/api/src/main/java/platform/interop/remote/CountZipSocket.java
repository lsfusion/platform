package platform.interop.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class CountZipSocket extends Socket {
    private CompressedBlockInputStream in;
    private CompressedBlockOutputStream out;
    private ISocketTrafficSum observer;

    public CountZipSocket() {
        super();
    }

    public CountZipSocket(String host, int port)
            throws IOException {
        super(host, port);
    }

    public void setObserver(ISocketTrafficSum observer) {
        this.observer = observer;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (in == null) {
            in = new CompressedBlockInputStream(super.getInputStream());
            if (observer != null) {
                in.setObserver(observer);
            }
        }
        return in;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new CompressedBlockOutputStream(super.getOutputStream(), 1 << 20);
            if (observer != null) {
                out.setObserver(observer);
            }
        }
        return out;
    }

    @Override
    public synchronized void close() throws IOException {
        OutputStream o = getOutputStream();
        o.flush();
        super.close();
    }
}

