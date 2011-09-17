package platform.interop.remote;

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

public class CompressedBlockOutputStream extends FilterOutputStream {

    private ISocketTrafficSum observer;

    public void setObserver(ISocketTrafficSum observer) {
        this.observer = observer;
    }

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
    private int len = 0;

    /**
     * Deflater for compressing data
     */
    private Deflater deflater = null;

    /**
     * Constructs a CompressedBlockOutputStream that writes to
     * the given underlying output stream 'os' and sends a compressed
     * block once 'size' byte have been written. The default
     * compression strategy and level are used.
     */
    public CompressedBlockOutputStream(OutputStream os, int size)
            throws IOException {
        this(os, size,
                Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY);
    }

    /**
     * Constructs a CompressedBlockOutputStream that writes to the
     * given underlying output stream 'os' and sends a compressed
     * block once 'size' byte have been written. The compression
     * level and strategy should be specified using the constants
     * defined in java.util.zip.Deflator.
     */
    public CompressedBlockOutputStream(OutputStream os, int size,
                                       int level, int strategy) throws IOException {
        super(os);
        this.inBuf = new byte[size];
        this.outBuf = new byte[size + 65536];
        this.deflater = new Deflater(level);
        this.deflater.setStrategy(strategy);
    }

    protected void compressAndSend() throws IOException {
        if (len > 0) {
            deflater.setInput(inBuf, 0, len);
            deflater.finish();
            int size = deflater.deflate(outBuf, 8, outBuf.length - 8);

            if (observer != null) {
                observer.incrementOut(size);
            }
            // Write the size of the compressed data, followed
            // by the size of the uncompressed data

            outBuf[0] = (byte)(size >>> 24);
            outBuf[1] = (byte)(size >>> 16);
            outBuf[2] = (byte)(size >>> 8);
            outBuf[3] = (byte)(size);

            outBuf[4] = (byte)(len >>> 24);
            outBuf[5] = (byte)(len >>> 16);
            outBuf[6] = (byte)(len >>> 8);
            outBuf[7] = (byte)(len);

            out.write(outBuf, 0, size + 8);
            out.flush();

            len = 0;
            deflater.reset();
        }
    }

    public void write(int b) throws IOException {
        inBuf[len++] = (byte) b;
        if (len == inBuf.length) {
            compressAndSend();
        }
    }

    public void write(byte[] b, int boff, int blen)
            throws IOException {
        while ((len + blen) > inBuf.length) {
            int toCopy = inBuf.length - len;
            System.arraycopy(b, boff, inBuf, len, toCopy);
            len += toCopy;
            compressAndSend();
            boff += toCopy;
            blen -= toCopy;
        }
        System.arraycopy(b, boff, inBuf, len, blen);
        len += blen;
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
