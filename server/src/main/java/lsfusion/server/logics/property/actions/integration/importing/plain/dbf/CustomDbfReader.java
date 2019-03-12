package lsfusion.server.logics.property.actions.integration.importing.plain.dbf;

import lsfusion.base.file.RawFileData;
import net.iryndin.jdbf.core.DbfMetadata;
import net.iryndin.jdbf.util.DbfMetadataUtils;

import java.io.*;
import java.util.Arrays;

public class CustomDbfReader implements Closeable {

    private InputStream dbfInputStream;
    private CustomMemoReader memoReader;
    private DbfMetadata metadata;
    private byte[] oneRecordBuffer;
    private int recordsCounter = 0;
    private static final int BUFFER_SIZE = 8192;

    public CustomDbfReader(RawFileData dbfFile, RawFileData memoFile) throws IOException {
        this.dbfInputStream = new BufferedInputStream(dbfFile.getInputStream(), BUFFER_SIZE);
        if(memoFile != null)
            this.memoReader = new CustomMemoReader(memoFile);
        readMetadata();
    }

    public DbfMetadata getMetadata() {
        return metadata;
    }

    private void readMetadata() throws IOException {
        this.dbfInputStream.mark(1024*1024);
        metadata = new DbfMetadata();
        readHeader();
        
        DbfMetadataUtils.readFields(metadata, dbfInputStream);

        oneRecordBuffer = new byte[metadata.getOneRecordLength()];

        findFirstRecord();
    }

    private void readHeader() {
        try {
            // 1. Allocate buffer
            byte[] bytes = new byte[16];
            // 2. Read 16 bytes
            dbfInputStream.read(bytes);
            // 3. Fill header fields
            DbfMetadataUtils.fillHeaderFields(metadata, bytes);
            // 4. Read next 16 bytes (for most DBF types these are reserved bytes)
            dbfInputStream.read(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read header. Possible, corrupted or incorrect DBF", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (memoReader != null) {
            memoReader.close();
            memoReader = null;
        }
        if (dbfInputStream != null) {
            dbfInputStream.close();
            dbfInputStream = null;
        } //delete file
        metadata = null;
        recordsCounter = 0;
    }

    public void findFirstRecord() throws IOException {
        seek(dbfInputStream, metadata.getFullHeaderLength());
    }

    private void seek(InputStream inputStream, int position) throws IOException {
        inputStream.reset();
        inputStream.skip(position);
    }

    public CustomDbfRecord read() throws IOException {
        Arrays.fill(oneRecordBuffer, (byte)0x0);
        int readLength = dbfInputStream.read(oneRecordBuffer);

        if (readLength < metadata.getOneRecordLength()) {
            return null;
        }

        return createDbfRecord();
    }

    private CustomDbfRecord createDbfRecord() {
        return new CustomDbfRecord(oneRecordBuffer, metadata, memoReader, ++recordsCounter);
    }
}