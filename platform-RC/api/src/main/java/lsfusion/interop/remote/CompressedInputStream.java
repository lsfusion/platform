package lsfusion.interop.remote;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class CompressedInputStream extends InflaterInputStream {

    private final CompressedStreamObserver observer;

    public volatile boolean hangs = false;

    public CompressedInputStream(InputStream is, int size, CompressedStreamObserver observer) throws IOException {
        super(is, new Inflater(false), size);
        this.observer = observer;
    }

    @Override
    protected void fill() throws IOException {
        hangs = true;
        super.fill();
        hangs = false;
        if (observer != null && len > 0) {
            observer.bytesReaden(len);
        }
    }
}
