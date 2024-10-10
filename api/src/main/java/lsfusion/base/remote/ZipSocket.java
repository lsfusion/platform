package lsfusion.base.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class ZipSocket extends Socket {

    private static final boolean NOWRAP = true;
    private static final int BUFFER_SIZE = 8192;
    private InputStream in;
    private OutputStream out;

    public ZipSocket(SocketImpl impl) throws SocketException {
        super(impl);
    }

    public ZipSocket(String host, int port) throws IOException {
        super(host, port);
    }

    private OutputStream createOut() throws IOException {
//        return super.getOutputStream();
        return new DeflaterOutputStream(super.getOutputStream(), new Deflater(Deflater.NO_COMPRESSION, NOWRAP), BUFFER_SIZE, true);
    }

    private InputStream createIn() throws IOException {
//        return super.getInputStream();
        return new InflaterInputStream(super.getInputStream(), new Inflater(NOWRAP), BUFFER_SIZE);
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
            InputStream inStream = in;
            in = null;
            inStream.close();
        }
        if (out != null) {
            OutputStream outStream = out;
            out = null;
            outStream.close();
        }
    }
}

