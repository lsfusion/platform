package platform.base;

import java.io.*;

public class IOUtils {
    public static final int BUFFER_SIZE = 16384;

    public static byte[] readBytesFromStream(InputStream in) throws IOException {
        return readBytesFromStream(in, Integer.MAX_VALUE);
    }

    public static byte[] readBytesFromStream(InputStream in, int maxLength) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte buffer[] = new byte[BUFFER_SIZE];

        int left = maxLength;
        int readCount;
        while (left > 0 && (readCount = in.read(buffer, 0, Math.min(buffer.length, left))) != -1) {
            out.write(buffer, 0, readCount);
            left -= readCount;
        }

        return out.toByteArray();
    }

    //TODO : сделать нормальные close (перенести его в finally)
    public static byte[] getFileBytes(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        byte[] result = readBytesFromStream(in);
        in.close();
        return result;
    }

    public static void putFileBytes(File file, byte[] array) throws IOException {
        if (file.getParentFile().exists())
            file.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(file);
        out.write(array);
        out.close();
    }
}
