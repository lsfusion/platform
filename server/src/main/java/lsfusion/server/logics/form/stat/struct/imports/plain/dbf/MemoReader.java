package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

import lsfusion.base.file.RawFileData;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reader of memo files (tested of *.FPT files - Visual FoxPro)
 * See links:
 *
 * Visual FoxPro file formats:
 * http://msdn.microsoft.com/en-us/library/aa977077(v=vs.71).aspx
 *
 * DBase file formats:
 * http://www.dbase.com/Knowledgebase/INT/db7_file_fmt.htm
 *
 */
public class MemoReader implements Closeable {

    private static final int BUFFER_SIZE = 8192;
    private static final int MEMO_HEADER_LENGTH = 0x200; // 512 bytes

    private RawFileData memoFile;
    private int blockSize;

    public MemoReader(RawFileData memoFile) throws IOException {
        this.memoFile = memoFile;
        readMetadata();
    }

    private void readMetadata() throws IOException {
        byte[] headerBytes = new byte[MEMO_HEADER_LENGTH];
        try(InputStream memoInputStream = new BufferedInputStream(memoFile.getInputStream(), BUFFER_SIZE)) {
            memoInputStream.mark(8192);
            memoInputStream.read(headerBytes);
            this.blockSize = BitUtils.makeInt(headerBytes[7], headerBytes[6]);
        }
    }

    @Override
    public void close() {
    }

    public byte[] read(int offsetInBlocks) throws IOException {
        InputStream memoInputStream = new BufferedInputStream(memoFile.getInputStream(), BUFFER_SIZE);
        memoInputStream.skip(blockSize * offsetInBlocks);
        byte[] recordHeader = new byte[8];
        memoInputStream.read(recordHeader);
        int memoRecordLength = BitUtils.makeInt(recordHeader[7], recordHeader[6], recordHeader[5], recordHeader[4]);
        if(memoRecordLength < 0 || memoRecordLength > 100000000) {
            memoRecordLength = 0;
        }
        byte[] recordBody = new byte[memoRecordLength];
        memoInputStream.read(recordBody);
        memoInputStream.close();
        return recordBody;
    }
}
