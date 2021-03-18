package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

import lsfusion.server.logics.classes.data.ParseException;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.core.DbfFieldTypeEnum;
import net.iryndin.jdbf.core.DbfMetadata;
import net.iryndin.jdbf.util.BitUtils;
import net.iryndin.jdbf.util.JdbfUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.*;

public class CustomDbfRecord {

    public static final String NUMERIC_OVERFLOW = "*";

    private byte[] bytes;
    private DbfMetadata metadata;
    private CustomMemoReader memoReader;
    private Charset stringCharset;
    private final int recordNumber;

    public CustomDbfRecord(byte[] source, DbfMetadata metadata, CustomMemoReader memoReader, int recordNumber) {
        this.recordNumber = recordNumber;
        this.bytes = new byte[source.length];
        System.arraycopy(source, 0, this.bytes, 0, source.length);
        this.metadata = metadata;
        this.memoReader = memoReader;
    }

    /*
    public CustomDbfRecord(DbfMetadata metadata) {
        this.metadata = metadata;
        fillBytesFromMetadata();
    }
    private void fillBytesFromMetadata() {
        bytes = new byte[metadata.getOneRecordLength()];
        BitUtils.memset(bytes, JdbfUtils.EMPTY);
    }
    */

    /**
     * Check if record is deleted.
     * According to documentation at
     * http://www.dbase.com/Knowledgebase/INT/db7_file_fmt.htm :
     * Data records are preceded by one byte, that is, a space (0x20) if the record is not deleted, an asterisk (0x2A) if the record is deleted.
     * So, if record is preceded by 0x2A - it is considered to be deleted
     * All other cases: record is considered to be not deleted
     * @return
     */
    public boolean isDeleted() {
        return this.bytes[0] == 0x2A;
    }

    public Charset getStringCharset() {
        return stringCharset;
    }

    public void setStringCharset(Charset stringCharset) {
        this.stringCharset = stringCharset;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    public String getString(String fieldName) {
        Charset charset = this.stringCharset;
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        return getString(fieldName, charset);
    }

    public String getString(String fieldName, String charsetName) {
        return getString(fieldName, Charset.forName(charsetName));
    }

    public String getString(String fieldName, Charset charset) {
        DbfField f = getField(fieldName);
        int actualOffset = f.getOffset();
        int actualLength = f.getLength();

        byte[] fieldBytes = new byte[actualLength];
        System.arraycopy(bytes, actualOffset, fieldBytes, 0, actualLength);

        // check for empty strings
        while ((actualLength > 0) && (bytes[actualOffset] == JdbfUtils.EMPTY)) {
            actualOffset++;
            actualLength--;
        }

        while ((actualLength > 0) && (bytes[actualOffset + actualLength - 1] == JdbfUtils.EMPTY)) {
            actualLength--;
        }

        if (actualLength == 0) {
            return null;
        }

        //0x00 byte is incorrect in postgre, so we replace it for space
        return new String(bytes, actualOffset, actualLength, charset).replace((char) 0x00, ' ');

		/*
        byte[] b = new byte[actualLength];
		System.arraycopy(bytes, actualOffset, b, 0, actualLength);
		// check for empty strings
		{
			for (int i = b.length-1; i>=0; i--) {
				if (b[i] == JdbfUtils.EMPTY) {
					actualLength--;
				} else {
					break;
				}
			}
			if (actualLength == 0) {
				return null;
			}
		}
		return new String(b, 0, actualLength, charset);
		*/
    }

    public byte[] getMemoAsBytes(String fieldName) throws IOException, ParseException {
        DbfField f = getField(fieldName);
        if (f.getType() != DbfFieldTypeEnum.Memo) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not MEMO field!");
        }
        int offsetInBlocks = 0;
        if (f.getLength() == 10) {
            offsetInBlocks = getBigDecimal(fieldName).intValueExact();
        } else {
            byte[] dbfFieldBytes = new byte[f.getLength()];
            System.arraycopy(bytes, f.getOffset(), dbfFieldBytes, 0, f.getLength());
            offsetInBlocks = BitUtils.makeInt(dbfFieldBytes[0],dbfFieldBytes[1],dbfFieldBytes[2],dbfFieldBytes[3]);
        }
        if (offsetInBlocks == 0) return new byte[0];
        return memoReader.read(offsetInBlocks).getValue();
    }

    public String getMemoAsString(String fieldName, Charset charset) throws IOException, ParseException {
        DbfField f = getField(fieldName);
        if (f.getType() != DbfFieldTypeEnum.Memo) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not MEMO field!");
        }
        int offsetInBlocks = 0;
        if (f.getLength() == 10) {
            BigDecimal bd = getBigDecimal(fieldName);
            offsetInBlocks = bd != null ? bd.intValueExact() : 0;
        } else {
            byte[] dbfFieldBytes = new byte[f.getLength()];
            System.arraycopy(bytes, f.getOffset(), dbfFieldBytes, 0, f.getLength());
            offsetInBlocks = BitUtils.makeInt(dbfFieldBytes[0],dbfFieldBytes[1],dbfFieldBytes[2],dbfFieldBytes[3]);
        }
        if (offsetInBlocks == 0) return "";
        assert memoReader != null;
        return memoReader.read(offsetInBlocks).getValueAsString(charset);
    }

    public String getMemoAsString(String fieldName) throws IOException, ParseException {
        Charset charset = getStringCharset();
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        return getMemoAsString(fieldName, charset);
    }

    public Date getDate(String fieldName) throws java.text.ParseException {
        String s = getString(fieldName);
        if (s == null) {
            return null;
        }
        try {
            return JdbfUtils.parseDate(s);
        } catch (java.text.ParseException e) {
            try {
                //Дата может присылаться в формате кол-ва дней от неизвестной даты 3 байтами в обратном порядке
                byte[] bytes = getBytes(fieldName);
                int days = formatByte(bytes[2]) * 256 * 256 + formatByte(bytes[1]) * 256 + formatByte(bytes[0]);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(0);
                cal.add(Calendar.DATE, days - 2440588); //значение для 01.01.1970
                return cal.getTime();
            } catch (Exception e1) {
                //если не получилось, шлём первоначальный exception
                throw e;
            }
        }
    }

    private int formatByte(byte b) {
        return b < 0 ? (256 + b) : b;
    }

    public Number getNumber(String fieldName) throws ParseException {
        DbfField f = getField(fieldName);
        switch (f.getType()) {
            case Integer:
                return getInteger(fieldName);
            case Double:
                return getDouble(fieldName);
            default:
                return getBigDecimal(fieldName);
        }
    }
    
    public BigDecimal getBigDecimal(String fieldName) throws ParseException {
        DbfField f = getField(fieldName);
        String s = getString(fieldName);

        if (s == null || s.trim().length() == 0) {
            return null;
        } else {
            s = s.trim();
        }

        if (s.contains(NUMERIC_OVERFLOW)) {
            return null;
        }

        //MathContext mc = new MathContext(f.getNumberOfDecimalPlaces());
        //return new BigDecimal(s, mc);
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            throw ParseException.propagateWithMessage(String.format("Error parsing numeric %s (row %s, column %s)", s, recordNumber, fieldName), e);
        }
    }

    public Double getDouble(String fieldName) throws ParseException {
        try {
            byte[] bytes = getBytes(fieldName);
            reverse(bytes);
            try(DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return stream.readDouble();
            }
        } catch (Exception e) {
            throw ParseException.propagateWithMessage(String.format("Error parsing double (row %s, column %s)", fieldName, recordNumber), e);
        }
    }

    private void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    public Boolean getBoolean(String fieldName) {
        String s = getString(fieldName);
        if (s == null) {
            return null;
        }
        if (s.equalsIgnoreCase("t")) {
            return Boolean.TRUE;
        } else if (s.equalsIgnoreCase("f")) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }

    public void setBoolean(String fieldName, Boolean value) {
        DbfField f = getField(fieldName);
        // TODO: write boolean
    }

    public byte[] getBytes(String fieldName) {
        DbfField f = getField(fieldName);
        byte[] b = new byte[f.getLength()];
        System.arraycopy(bytes, f.getOffset(), b, 0, f.getLength());
        return b;
    }

    public void setBytes(String fieldName, byte[] fieldBytes) {
        DbfField f = getField(fieldName);
        // TODO:
        // assert fieldBytes.length = f.getLength()
        System.arraycopy(fieldBytes, 0, bytes, f.getOffset(), f.getLength());
    }

    private Integer getInteger(String fieldName) throws ParseException {
        try {
            byte[] bytes = getBytes(fieldName);
            return BitUtils.makeInt(bytes[0], bytes[1], bytes[2], bytes[3]);
        } catch (Exception e) {
            throw ParseException.propagateWithMessage(String.format("Error parsing integer (row %s, column %s)", fieldName, recordNumber), e);
        }
    }

    public DbfField getField(String fieldName) {
        return metadata.getField(fieldName);
    }

    public Collection<DbfField> getFields() {
        return metadata.getFields();
    }

    public String getStringRepresentation() throws Exception {
        StringBuilder sb = new StringBuilder(bytes.length * 10);
        for (DbfField f : getFields()) {
            sb.append(f.getName()).append("=");
            switch (f.getType()) {
                case Character: {
                    //String s = getString(f.getName(), "Cp866");
                    String s = getString(f.getName());
                    //System.out.println(f.getName()+"="+s);
                    sb.append(s);
                    break;
                }
                case Date: {
                    Date d = getDate(f.getName());
                    //System.out.println(f.getName()+"="+d);
                    sb.append(d);
                    break;
                }
                case Numeric: {
                    BigDecimal bd = getBigDecimal(f.getName());
                    //System.out.println(f.getName()+"="+(bd != null ? bd.toPlainString() : null));
                    sb.append(bd);
                    break;
                }
                case Logical: {
                    Boolean b = getBoolean(f.getName());
                    //System.out.println(f.getName()+"="+b);
                    sb.append(b);
                    break;
                }
            }
            sb.append(", ");
        }
        return sb.toString();
    }

    public Map<String, Object> toMap() throws ParseException, java.text.ParseException {
        Map<String, Object> map = new LinkedHashMap<>(getFields().size() * 2);

        for (DbfField f : getFields()) {
            String name = f.getName();

            switch (f.getType()) {

                case Character:
                    map.put(name, getString(name));
                    break;

                case Date:
                    map.put(name, getDate(name));
                    break;

                case Numeric:
                    map.put(name, getBigDecimal(name));
                    break;

                case Logical:
                    map.put(name, getBoolean(name));
                    break;

                case Integer:
                    map.put(name, getInteger(name));
                    break;
            }
        }

        return map;
    }
}