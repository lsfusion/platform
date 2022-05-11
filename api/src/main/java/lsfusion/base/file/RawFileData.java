package lsfusion.base.file;

import lsfusion.base.BaseUtils;
import lsfusion.base.ResourceUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.mutability.TwinImmutableObject;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class RawFileData extends TwinImmutableObject<RawFileData> implements Serializable {

    public static final RawFileData EMPTY = new RawFileData(new byte[0]);
    
    private final byte[] array;

    public RawFileData(byte[] array) {
        assert array != null;
        this.array = array;
    }

    public RawFileData(InputStream stream) throws IOException {
        this.array = IOUtils.readBytesFromStream(stream);
    }

    public RawFileData(ByteArrayOutputStream array) {
        this.array = array.toByteArray();
    }
    
    public RawFileData(File file) throws IOException {
        this.array = IOUtils.getFileBytes(file);
    }

    public RawFileData(String filePath) throws IOException {
        this.array = IOUtils.getFileBytes(filePath);
    }

    public RawFileData(String resourcePath, boolean resource) throws IOException {
        this(ResourceUtils.getResourceAsStream(resourcePath));
        assert resource;
    }

    public byte[] getBytes() {
        return array;
    }
    
    public int getLength() {
        return array.length;
    }
    
    public void write(OutputStream out) throws IOException {
        out.write(array);
    }

    public void write(File file) throws IOException {
        FileUtils.writeByteArrayToFile(file, array);
    }

    public void append(String filePath) throws IOException {
        Files.write(Paths.get(filePath), array, StandardOpenOption.APPEND);
    }

    public void write(String filePath) throws IOException {
        try(FileOutputStream fos = new FileOutputStream(filePath)) {
            write(fos);
        }
    }

    // cache ???
    public ImageIcon getImageIcon() {
        return new ImageIcon(array);
    }

    private String ID;
    public String getID() {
        if(ID == null)
            ID = SystemUtils.generateID(array);
        return ID;
    }
    
    public InputStream getInputStream() {
        return new ByteArrayInputStream(array);
    }        

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return Arrays.equals(array, ((RawFileData) o).array);
    }

    @Override
    public int immutableHashCode() {
        return Arrays.hashCode(array);
    }

    public static RawFileData impossible = new RawFileData(BaseUtils.impossibleString.getBytes());
}
