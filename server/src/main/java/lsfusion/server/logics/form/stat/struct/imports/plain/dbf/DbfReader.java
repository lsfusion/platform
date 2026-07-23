// header/field reading copied from net.iryndin jdbf (https://github.com/iryndin/jdbf, Apache License 2.0) and stripped down to what the DBF import uses
package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

import lsfusion.base.file.RawFileData;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DbfReader implements Closeable {

    private static final int BUFFER_SIZE = 8192;
    private static final int FIELD_RECORD_LENGTH = 32;
    private static final int HEADER_TERMINATOR = 0x0D;

    private InputStream dbfInputStream;
    private MemoReader memoReader;
    private DbfMetadata metadata;
    private byte[] oneRecordBuffer;
    private int recordsCounter = 0;

    public DbfReader(RawFileData dbfFile, RawFileData memoFile) throws IOException {
        this.dbfInputStream = new BufferedInputStream(dbfFile.getInputStream(), BUFFER_SIZE);
        if(memoFile != null)
            this.memoReader = new MemoReader(memoFile);
        readMetadata();
    }

    public DbfMetadata getMetadata() {
        return metadata;
    }

    private void readMetadata() throws IOException {
        this.dbfInputStream.mark(1024*1024);
        metadata = new DbfMetadata();
        readHeader();

        readFields();

        oneRecordBuffer = new byte[metadata.getOneRecordLength()];

        findFirstRecord();
    }

    private void readHeader() {
        try {
            // 1. Allocate buffer
            byte[] bytes = new byte[16];
            // 2. Read 16 bytes
            dbfInputStream.read(bytes);
            // 3. Fill header fields (the update date, records quantity and flags are not used, so they are not read)
            metadata.setType(DbfMetadata.Type.fromInt(bytes[0]));
            metadata.setFullHeaderLength(BitUtils.makeInt(bytes[8], bytes[9]));
            metadata.setOneRecordLength(BitUtils.makeInt(bytes[10], bytes[11]));
            // 4. Read next 16 bytes (for most DBF types these are reserved bytes)
            dbfInputStream.read(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read header. Possible, corrupted or incorrect DBF", e);
        }
    }

    private void readFields() throws IOException {
        List<DbfField> fields = new ArrayList<>();
        byte[] fieldBytes = new byte[FIELD_RECORD_LENGTH];
        while (true) {
            if (dbfInputStream.read(fieldBytes) != FIELD_RECORD_LENGTH)
                throw new IOException("The file is corrupted or is not a dbf file");

            fields.add(createDbfField(fieldBytes));

            long oldAvailable = dbfInputStream.available();
            int terminator = dbfInputStream.read();
            if (terminator == -1) {
                throw new IOException("The file is corrupted or is not a dbf file");
            } else if (terminator == HEADER_TERMINATOR) {
                break;
            } else {
                dbfInputStream.reset();
                dbfInputStream.skip(dbfInputStream.available() - oldAvailable);
            }
        }
        metadata.setFields(fields);
    }

    private static DbfField createDbfField(byte[] fieldBytes) {
        DbfField field = new DbfField();
        // 1. Set name (the first 11 bytes, zero-padded when shorter)
        {
            int nameLength = 0;
            while (nameLength < 11 && fieldBytes[nameLength] > 0)
                nameLength++;
            field.setName(new String(fieldBytes, 0, nameLength));
        }
        // 2. Set type
        field.setType(DbfField.Type.fromChar((char) fieldBytes[11]));
        // 3. Set length
        {
            int length = fieldBytes[16];
            if (length < 0) {
                length = 256 + length;
            }
            field.setLength(length);
        }
        return field;
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

    public DbfRecord read() throws IOException {
        Arrays.fill(oneRecordBuffer, (byte)0x0);
        int readLength = dbfInputStream.read(oneRecordBuffer);

        if (readLength < metadata.getOneRecordLength()) {
            return null;
        }

        return new DbfRecord(oneRecordBuffer, metadata, memoReader, ++recordsCounter);
    }
}