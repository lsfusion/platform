package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import org.apache.commons.net.util.Base64;

import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class DynamicFormatFileClass extends AbstractDynamicFormatFileClass<FileData> {

    @Override
    protected String getFileSID() {
        return "FILE"; // для обратной совместимости такое название
    }

    @Override
    public FileData getDefaultValue() {
        return FileData.EMPTY;
    }

    @Override
    public Class getReportJavaClass() {
        return FileData.class;
    }

    private static Collection<DynamicFormatFileClass> instances = new ArrayList<>();

    public static DynamicFormatFileClass get() {
        return get(false, false);
    }
    public static DynamicFormatFileClass get(boolean multiple, boolean storeName) {
        for (DynamicFormatFileClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        DynamicFormatFileClass instance = new DynamicFormatFileClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private DynamicFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DynamicFormatFileClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.DYNAMICFORMATFILE;
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if (typeFrom instanceof NamedFileClass) {
            return "cast_named_file_to_dynamic_file(" + value + ")";
        } else if (typeFrom instanceof StaticFormatFileClass) {
            String extension = ((StaticFormatFileClass) typeFrom).getExtension();
            return "cast_static_file_to_dynamic_file(" + value + ", " + (extension != null ? ("'" + extension + "'") : "null") + ")";
        } else if (typeFrom instanceof JSONClass) { // important to make auto import work (it uses extension(FILE()))
            return "cast_json_to_dynamic_file(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    protected FileData parseHTTPNotNullString(String s, Charset charset) {
        return new FileData(new RawFileData(s.getBytes(charset)), "txt");
    }

    @Override
    protected FileData parseHTTPNotNull(FileData b) {
        return b;
    }

    @Override
    protected String formatHTTPNotNullString(FileData value, Charset charset) {
        return value != null ? new String(value.getRawFile().getBytes(), charset) : null;
    }

    @Override
    protected FileData formatHTTPNotNull(FileData b) {
        return b;
    }

    @Override
    public FileData writePropNotNull(RawFileData value, String extension) {
        return new FileData(value, extension);
    }

    @Override
    public RawFileData readPropNotNull(FileData value, String charset) {
        return value.getRawFile();
    }

    @Override
    public FileData read(Object value) {
        if(value instanceof byte[])
            return new FileData((byte[]) value);
        return (FileData) value;
    }

    @Override
    public FileData read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        byte[] result = set.getBytes(name);
        if(result != null)
            return new FileData(result);
        return null;
    }

    @Override
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, value != null ? ((FileData) value).getBytes() : null);
    }

    @Override
    public int getSize(FileData value) {
        return value.getLength();
    }

    @Override
    public FileData parseString(String s) {
        return new FileData(Base64.decodeBase64(s));
    }

    @Override
    protected byte[] getBytes(FileData value) {
        return value.getBytes();
    }
}