package lsfusion.base.file;

import com.google.common.primitives.Bytes;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.session.ExternalUtils;

import java.io.Serializable;

public class FileData extends TwinImmutableObject<FileData> implements Serializable {

    private final RawFileData fileData;
    private final String extension;
    
    public final static FileData EMPTY = new FileData(RawFileData.EMPTY, "");
    public final static FileData NULL = new FileData(RawFileData.EMPTY, "null");
    public boolean isNull() {
        return getExtension().equals("null");
    }

    public FileData(RawFileData fileData, String extension) {
        assert fileData != null;
        this.fileData = fileData;
        this.extension = extension;
    }

    public static byte[] getNameBytes(String name) {
        return name.getBytes(ExternalUtils.fileDataNameCharset);
    }
    public static String getNameString(byte[] bytes) {
        return new String(bytes, ExternalUtils.fileDataNameCharset);
    }

    public FileData(byte[] array) {
        byte ext[] = new byte[array[0]];
        System.arraycopy(array, 1, ext, 0, ext.length);
        extension = getNameString(ext);

        byte fileArray[] = new byte[array.length - array[0] - 1];
        System.arraycopy(array, 1 + array[0], fileArray, 0, fileArray.length);
        fileData = new RawFileData(fileArray);
    }
    
    public RawFileData getRawFile() {
        return fileData;
    }
    public String getExtension() {
        return extension;
    }    
    
    public int getLength() {
        return getNameBytes(extension).length + 1 + fileData.getLength();
    }

    public byte[] getBytes() {
        byte[] fileBytes = fileData.getBytes();
        byte[] extensionBytes = getNameBytes(extension);
        return Bytes.concat(new byte[] {(byte) extensionBytes.length}, extensionBytes, fileBytes);
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return fileData.equals(((FileData) o).fileData) && extension.equals(((FileData) o).extension);
    }

    @Override
    public int immutableHashCode() {
        return 31 * fileData.hashCode() + extension.hashCode();
    }
}
