package lsfusion.base.file;

import com.google.common.primitives.Bytes;
import lsfusion.base.mutability.TwinImmutableObject;

import java.io.Serializable;

public class NamedFileData extends TwinImmutableObject<NamedFileData> implements Serializable {

    private final FileData fileData;
    private final String name; //without extension

    public final static NamedFileData EMPTY = new NamedFileData(FileData.EMPTY, "");

    public NamedFileData(FileData fileData, String name) {
        assert fileData != null;
        this.fileData = fileData;
        this.name = name;
    }

    public NamedFileData(byte[] array) {
        byte[] nameBytes = new byte[array[0]];
        System.arraycopy(array, 1, nameBytes, 0, nameBytes.length);
        name = new String(nameBytes);

        byte[] fileDataArray = new byte[array.length - array[0] - 1];
        System.arraycopy(array, 1 + array[0], fileDataArray, 0, fileDataArray.length);
        fileData = new FileData(fileDataArray);
    }
    
    public RawFileData getRawFile() {
        return fileData.getRawFile();
    }
    
    public int getLength() {
        return name.getBytes().length + 1 + fileData.getLength();
    }

    public byte[] getBytes() {
        byte[] fileBytes = fileData.getBytes();
        byte[] nameBytes = name.getBytes();
        return Bytes.concat(new byte[] {(byte) nameBytes.length}, nameBytes, fileBytes);
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return fileData.equals(((NamedFileData) o).fileData) && name.equals(((NamedFileData) o).name);
    }

    @Override
    public int immutableHashCode() {
        return 31 * fileData.hashCode() + name.hashCode();
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return fileData.getExtension();
    }
}