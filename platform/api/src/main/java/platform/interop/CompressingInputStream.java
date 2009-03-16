package platform.interop;

import java.util.zip.GZIPInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.DataInputStream;

public class CompressingInputStream extends GZIPInputStream {

    public CompressingInputStream(InputStream in) throws IOException {
        super(in);
    }
}
