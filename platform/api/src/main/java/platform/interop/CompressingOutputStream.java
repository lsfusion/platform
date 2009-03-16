package platform.interop;

import java.util.zip.GZIPOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.DataOutputStream;

public class CompressingOutputStream extends GZIPOutputStream {
    
    public CompressingOutputStream(OutputStream out) throws IOException {
        super(out);
    }
}
