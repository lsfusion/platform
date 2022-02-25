package lsfusion.base.file;

import com.google.common.primitives.Bytes;
import lsfusion.base.mutability.TwinImmutableObject;

import java.io.Serializable;

public class FileData extends TwinImmutableObject<FileData> implements Serializable {

    private final RawFileData fileData;
    private final String extension;
    
    public final static FileData EMPTY = new FileData(RawFileData.EMPTY, "");

    public FileData(RawFileData fileData, String extension) {
        assert fileData != null;
        this.fileData = fileData;
        this.extension = extension;
    }

    public FileData(byte[] array) {
        byte ext[] = new byte[array[0]];
        System.arraycopy(array, 1, ext, 0, ext.length);
        extension = new String(ext);

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
        return extension.getBytes().length + 1 + fileData.getLength();
    }

    public byte[] getBytes() {
        byte[] fileBytes = fileData.getBytes();
        byte[] extensionBytes = extension.getBytes();
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
