package lsfusion.base;

import java.io.OutputStream;

public class NullOutputStream extends OutputStream {

    public void write(int b) {
            // discard the data
    }

    public void write(byte[] b, int off, int len) {
            // discard the data
    }

    public void write(byte[] b) {
            // discard the data
    }
}
