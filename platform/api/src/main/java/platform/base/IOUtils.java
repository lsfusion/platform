package platform.base;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
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

    public static byte[] getFileBytes(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            return readBytesFromStream(in);
        } finally {
            in.close();
        }
    }

    public static void putFileBytes(File file, byte[] array) throws IOException {
        putFileBytes(file, array, 0, array.length);
    }

    public static void putFileBytes(File file, byte[] array, int off, int len) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        OutputStream out = new FileOutputStream(file);
        try {
            out.write(array, off, len);
        } finally {
            out.close();
        }
    }

    public static void writeImageIcon(DataOutputStream outStream, ImageIcon image) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(outStream);
            out.writeObject(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
