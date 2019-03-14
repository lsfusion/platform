package lsfusion.server.logics.form.stat.integration.importing.plain.dbf;

import lsfusion.base.file.RawFileData;
import net.iryndin.jdbf.core.MemoFileHeader;
import net.iryndin.jdbf.core.MemoRecord;
import net.iryndin.jdbf.util.BitUtils;
import net.iryndin.jdbf.util.JdbfUtils;

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
public class CustomMemoReader implements Closeable {

    private static final int BUFFER_SIZE = 8192;
    private RawFileData memoFile;
    private MemoFileHeader memoHeader;

    public CustomMemoReader(RawFileData memoFile) throws IOException {
        this.memoFile = memoFile;
        readMetadata();
    }

    private void readMetadata() throws IOException {
        byte[] headerBytes = new byte[JdbfUtils.MEMO_HEADER_LENGTH];
        try(InputStream memoInputStream = new BufferedInputStream(memoFile.getInputStream(), BUFFER_SIZE)) {
            memoInputStream.mark(8192);
            memoInputStream.read(headerBytes);
            this.memoHeader = MemoFileHeader.create(headerBytes);
        }
    }

    @Override
    public void close() throws IOException {
    }

    public MemoFileHeader getMemoHeader() {
        return memoHeader;
    }

    public MemoRecord read(int offsetInBlocks) throws IOException {
        InputStream memoInputStream = new BufferedInputStream(memoFile.getInputStream(), BUFFER_SIZE);
        //System.out.println(this.memoHeader);
       // memoInputStream.mark(memoHeader.getBlockSize()*offsetInBlocks);
        memoInputStream.skip(memoHeader.getBlockSize()*offsetInBlocks);
        byte[] recordHeader = new byte[8];
        memoInputStream.read(recordHeader);
        int memoRecordLength = BitUtils.makeInt(recordHeader[7], recordHeader[6], recordHeader[5], recordHeader[4]);
        if(memoRecordLength < 0 || memoRecordLength > 100000000) {
            memoRecordLength = 0;
        }
        byte[] recordBody = new byte[memoRecordLength];
        memoInputStream.read(recordBody);
        memoInputStream.close();
        return new MemoRecord(recordHeader, recordBody, memoHeader.getBlockSize(), offsetInBlocks);
    }
}