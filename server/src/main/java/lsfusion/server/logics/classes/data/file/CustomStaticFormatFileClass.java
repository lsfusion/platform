package lsfusion.server.logics.classes.data.file;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.property.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class CustomStaticFormatFileClass extends StaticFormatFileClass {

    private String filterDescription;
    private ImSet<String> filterExtensions;

    protected String getFileSID() {
        if(filterExtensions.isEmpty())
            return "RAWFILE";
        return "FILE";
    }

    public static CustomStaticFormatFileClass get(String description, String extensions) {
        return get(false, false, description, extensions);
    }
    public static CustomStaticFormatFileClass get(boolean multiple, boolean storeName, String description, String extensions) {
        return get(multiple, storeName, description, SetFact.toExclSet(extensions.split(" ")));
    }

    private static List<CustomStaticFormatFileClass> instances = new ArrayList<>();
    
    public static CustomStaticFormatFileClass get() { // RAWFILE
        return get(false, false);
    }
    public static CustomStaticFormatFileClass get(boolean multiple, boolean storeName) {
        return get(multiple, storeName, "", SetFact.singleton(""));
    }
    public static CustomStaticFormatFileClass get(boolean multiple, boolean storeName, String description, ImSet<String> extensions) {
        if(extensions.contains("")) // если есть RAWFILE то и результат считаем RAWFILE
            extensions = SetFact.singleton("");
        
        for(CustomStaticFormatFileClass instance : instances)
            if(instance.multiple == multiple && instance.filterDescription.equals(description) && instance.filterExtensions.equals(extensions))
                return instance;

        CustomStaticFormatFileClass instance = new CustomStaticFormatFileClass(multiple, storeName, description, extensions);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private CustomStaticFormatFileClass(boolean multiple, boolean storeName, String filterDescription, ImSet<String> filterExtensions) {
        super(multiple, storeName);
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions;
    }

    @Override
    public String getOpenExtension(RawFileData file) {
        return filterExtensions.get(0);
    }

    @Override
    protected ImSet<String> getExtensions() {
        return filterExtensions;
    }

    @Override
    public String getSID() {
        return super.getSID() + "_filterDescription=" + filterDescription + "_" + Arrays.toString(filterExtensions.toArray(new String[filterExtensions.size()])) + "]";
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeUTF(filterDescription);
        outStream.writeInt(filterExtensions.size());
        for (String extension : filterExtensions) {
            outStream.writeUTF(extension);
        }
    }

    @Override
    public byte getTypeID() {
        return DataType.CUSTOMSTATICFORMATFILE;
    }

    public List<RawFileData> getMultipleFiles(Object value) {
        assert multiple;
        if (value == null)
            return new ArrayList<>();

        List<RawFileData> result = new ArrayList<>();

        ByteArrayInputStream byteInStream = new ByteArrayInputStream((byte[]) value);
        DataInputStream inStream = new DataInputStream(byteInStream);

        try {
            int cnt = inStream.readInt();
            for (int i = 0; i < cnt; i++) {
                int length = inStream.readInt();
                byte temp[] = new byte[length];
                inStream.readFully(temp);
                result.add(new RawFileData(temp));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }


    public Map<String, RawFileData> getMultipleNamedFiles(Object value) {
        if (!storeName)
            throw new RuntimeException("Ошибка: файлы без имени");
        if (value == null)
            return new HashMap<>();
        assert multiple;

        Map<String, RawFileData> result = new HashMap<>();

        ByteArrayInputStream byteInStream = new ByteArrayInputStream((byte[]) value);
        try(DataInputStream inStream = new DataInputStream(byteInStream)) {
            int cnt = inStream.readInt();
            for (int i = 0; i < cnt; i++) {
                int nameLength = inStream.readInt();
                byte[] nameTemp = new byte[nameLength];
                inStream.readFully(nameTemp);
                int length = inStream.readInt();
                byte temp[] = new byte[length];
                inStream.readFully(temp);
                result.put(new String(nameTemp), new RawFileData(temp));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}
