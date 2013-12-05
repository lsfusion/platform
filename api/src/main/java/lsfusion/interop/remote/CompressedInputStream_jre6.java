package lsfusion.interop.remote;

/**
 * Input stream that decompresses data.
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
 * $Id:  1.2 2005/10/26 17:40:19 isenhour Exp $
 */

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class CompressedInputStream_jre6 extends FilterInputStream {

    private final CompressedStreamObserver observer;

    private byte[] lenBuf = null;

    /**
     * Buffer of compressed data read from the stream
     */
    private byte[] decompressedBuf = null;

    /**
     * Length of data in the input data
     */
    private int decLength = 0;

    /**
     * Buffer of uncompressed data
     */
    private byte[] compressedBuf = null;

    /**
     * Offset and length of uncompressed data
     */
    private int outOffs = 0;

    /**
     * Inflater for decompressing
     */
    private Inflater inflater = null;

    public CompressedInputStream_jre6(InputStream is, CompressedStreamObserver observer) throws IOException {
        super(is);
        this.observer = observer;
        inflater = new Inflater();
    }

    public boolean hangs = false;

    private void readAndDecompress() throws IOException {
        // Read the length of the compressed block

        if (lenBuf == null) {
            lenBuf = new byte[8];
        }

        hangs = true;
        blockRead(lenBuf, 0, 8);
        hangs = false;

        decLength = getIntFromBytes(lenBuf, 0);
        int cLength = getIntFromBytes(lenBuf, 4);

        int readenSum = 8;

        if ((decompressedBuf == null) || (decLength > decompressedBuf.length)) {
            decompressedBuf = new byte[decLength];
        }

        int inflated = 0;
        while (cLength != 0) {
            if ((compressedBuf == null) || (cLength > compressedBuf.length)) {
                compressedBuf = new byte[cLength];
            }
            blockRead(compressedBuf, 0, cLength);
            readenSum += cLength;

            inflater.setInput(compressedBuf, 0, cLength);
            try {
                while (!inflater.needsInput() && inflated < decLength) {
                    inflated += inflater.inflate(decompressedBuf, inflated, decLength - inflated);
                }
            } catch (DataFormatException dfe) {
                throw new IOException("Data format exception: ", dfe);
            }

            inflater.reset();
            blockRead(lenBuf, 0, 4);
            cLength = getIntFromBytes(lenBuf, 0);
            readenSum += 4;
        }

        outOffs = 0;

        if (observer != null) {
            observer.bytesReaden(readenSum);
        }
    }

    private void blockRead(byte[] buf, int off, int len) throws IOException {
        hangs = true;
        int readen = 0;
        while (readen < len) {
            int inCount = in.read(buf, off + readen, len - readen);
            if (inCount == -1) {
                hangs = false;
                throw new EOFException();
            }
            readen += inCount;
        }
        hangs = false;
    }

    private int getIntFromBytes(byte[] buf, int off) {
        return (buf[off] << 24) + ((buf[off + 1] & 0xFF) << 16) +
               ((buf[off + 2] & 0xFF) << 8) + (buf[off + 3] & 0xFF);
    }

    public int read() throws IOException {
        if (outOffs >= decLength) {
            try {
                readAndDecompress();
            } catch (EOFException eof) {
                return -1;
            }
        }

        return decompressedBuf[outOffs++] & 0xff;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int count = 0;
        while (count < len) {
            if (outOffs >= decLength) {
                try {
                    // If we've read at least one decompressed
                    // byte and further decompression would
                    // require blocking, return the count.
                    if ((count > 0) && (in.available() == 0)) {
                        return count;
                    } else {
                        readAndDecompress();
                    }
                } catch (EOFException eof) {
                    if (count == 0) {
                        count = -1;
                    }

                    return count;
                }
            }

            int toCopy = Math.min(decLength - outOffs, len - count);
            System.arraycopy(decompressedBuf, outOffs, b, off + count, toCopy);
            outOffs += toCopy;
            count += toCopy;
        }

        return count;
    }

    public int available() throws IOException {
        // This isn't precise, but should be an adequate
        // lower bound on the actual amount of available data
        return (decLength - outOffs) + in.available();
    }
}

