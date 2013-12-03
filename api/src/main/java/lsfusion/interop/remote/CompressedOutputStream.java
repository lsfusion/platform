package lsfusion.interop.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class CompressedOutputStream extends DeflaterOutputStream {

    private final CompressedStreamObserver observer;

    public CompressedOutputStream(OutputStream os, int size, CompressedStreamObserver observer)
            throws IOException {
        //TODO: NOT TESTED, JUST FIX COMPILE ERROR
//        super(os, new Deflater(Deflater.DEFAULT_COMPRESSION, false), size, true);
        super(os, new Deflater(Deflater.DEFAULT_COMPRESSION, false), size);
        this.observer = observer;
    }

    @Override
    protected void deflate() throws IOException {
        int len = def.deflate(buf, 0, buf.length);
        if (len > 0) {
            writeInnerBuf(len);
        }
    }

    @Override
    public void flush() throws IOException {
        if (!def.finished()) {
            int len;
            //TODO: NOT TESTED, JUST FIX COMPILE ERROR
            while ((len = def.deflate(buf, 0, buf.length)) > 0) {
//            while ((len = def.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH)) > 0) {
                writeInnerBuf(len);
                if (len < buf.length) {
                    break;
                }
            }
        }
        out.flush();
    }

    private void writeInnerBuf(int len) throws IOException {
        assert len > 0;

        out.write(buf, 0, len);

        if (observer != null) {
            observer.bytesWritten(len);
        }

    }
}
