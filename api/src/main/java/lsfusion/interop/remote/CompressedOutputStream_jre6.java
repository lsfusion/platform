package lsfusion.interop.remote;

/**
 * Output stream that compresses data. A compressed block
 * is generated and transmitted once a given number of bytes
 * have been written, or when the flush method is invoked.
 *
 * Copyright 2005 - Philip Isenhour - http://javatechniques.com/
 *
 * This software is provided 'as-is', without any express or
 * implied warranty. In no event will the authors be held liable
 * for any damages arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any
 * purpose, including commercial applications, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you
 *     must not claim that you wrote the original software. If you
 *     use this software in a product, an acknowledgment in the
 *     product documentation would be appreciated but is not required.
 *
 *  2. Altered source versions must be plainly marked as such, and
 *     must not be misrepresented as being the original software.
 *
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 *
 * $Id:  1.1 2005/10/26 17:19:05 isenhour Exp $
 */

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

public class CompressedOutputStream_jre6 extends FilterOutputStream {

    private final CompressedStreamObserver observer;

    /**
     * Buffer for input data
     */
    private byte[] inBuf = null;

    /**
     * Buffer for compressed data to be written
     */
    private byte[] outBuf = null;

    /**
     * Number of bytes in the buffer
     */
    private int inBufLen = 0;

    /**
     * Deflater for compressing data
     */
    private Deflater deflater = null;

    /**
     * Constructs a CompressedOutputStream that writes to
     * the given underlying output stream 'os' and sends a compressed
     * block once 'size' byte have been written. The default
     * compression strategy and level are used.
     */
    public CompressedOutputStream_jre6(OutputStream os, int size, CompressedStreamObserver observer) throws IOException {
        super(os);
        if (size < 4) {
            throw new IllegalArgumentException("size should be at least 4");
        }
        this.observer = observer;
        this.inBuf = new byte[size];
        this.outBuf = new byte[size];
        this.deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
    }

    protected void compressAndSend() throws IOException {
        compressAndSend(inBuf, 0, inBufLen);
        inBufLen = 0;

    }

    protected void compressAndSend(byte[] inBuf, int off, int len) throws IOException {
        if (len > 0) {
            deflater.setInput(inBuf, off, len);
            deflater.finish();

            int written;
            int compressedSize = 0;
            int extra = setIntAsBytes(outBuf, 0, len);
            while (!deflater.needsInput()) {
                written = deflater.deflate(outBuf, extra + 4, outBuf.length - extra - 4);
                if (written > 0) {
                    setIntAsBytes(outBuf, extra, written);

                    out.write(outBuf, 0, written + extra + 4);

                    compressedSize += written + extra + 4;
                    extra = 0;
                }
            }
            setIntAsBytes(outBuf, 0, 0);
            out.write(outBuf, 0, 4);

            deflater.reset();

            if (observer != null) {
                observer.bytesWritten(compressedSize);
            }
        }
    }

    private int setIntAsBytes(byte[] outBuf, int off, int value) {
        outBuf[off] = (byte) (value >>> 24);
        outBuf[off + 1] = (byte) (value >>> 16);
        outBuf[off + 2] = (byte) (value >>> 8);
        outBuf[off + 3] = (byte) (value);
        return 4;
    }

    public void write(int b) throws IOException {
        inBuf[inBufLen++] = (byte) b;
        if (inBufLen == inBuf.length) {
            compressAndSend();
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (inBufLen + len <= inBuf.length) {
            System.arraycopy(b, off, inBuf, inBufLen, len);
            inBufLen += len;
        } else {
            if (inBufLen != 0) {
                compressAndSend();
            }
            compressAndSend(b, off, len);
        }
    }

    public void flush() throws IOException {
        compressAndSend();
        out.flush();
    }

    public void close() throws IOException {
        compressAndSend();
        out.close();
    }
}
