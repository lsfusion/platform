package lsfusion.base;

import java.io.Serializable;
import java.util.Arrays;

public class FileData implements Serializable {

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
        
        byte[] extBytes = new byte[0];
        extBytes = new byte[extensionBytes.length + 1];
        extBytes[0] = (byte) extensionBytes.length;
        System.arraycopy(extensionBytes, 0, extBytes, 1, extensionBytes.length);
        byte[] result = new byte[extBytes.length + fileBytes.length];
        System.arraycopy(extBytes, 0, result, 0, extBytes.length);
        System.arraycopy(fileBytes, 0, result, extBytes.length, fileBytes.length);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof FileData && fileData.equals(((FileData) o).fileData) && extension.equals(((FileData) o).extension);
    }

    @Override
    public int hashCode() {
        return 31 * fileData.hashCode() + extension.hashCode();
    }

}
