package lsfusion.server.logics.classes.data.file;

import lsfusion.base.Result;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;

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
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if (typeFrom instanceof NamedFileClass) {
            return "cast_named_file_to_dynamic_file(" + value + ")";
        } else if(typeFrom instanceof DynamicFormatFileClass)
            return value;

        Result<String> rExtension = new Result<>();
        String castValue = StaticFormatFileClass.getCastToStatic(typeFrom, value, rExtension);
        if(castValue != null)
            return "cast_static_file_to_dynamic_file(" + value + ", '" + rExtension.result + "')";

        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }

    @Override
    protected FileData parseHTTPNotNull(FileData b, String charsetName, String fileName) {
        return b;
    }

    @Override
    protected FileData formatHTTPNotNull(FileData b, Charset charset, Result<String> fileName) {
        return b;
    }

    @Override
    public FileData writePropNotNull(NamedFileData value, String charset) {
        return value.getFileData();
    }

    @Override
    public NamedFileData readPropNotNull(FileData value, String charset) {
        return new NamedFileData(value);
    }

    @Override
    protected FileData readBytes(byte[] bytes) {
        return new FileData(bytes);
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
    protected FileData getValue(RawFileData data) {
        return new FileData(data, "dat");
    }

    @Override
    protected RawFileData getRawFileData(FileData value) {
        return value.getRawFile();
    }

    @Override
    public String getCastToStatic(String value) {
        return "cast_dynamic_file_to_static_file(" + value + ")";
    }
}