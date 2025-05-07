package lsfusion.server.logics.classes.data.file;

import lsfusion.base.Result;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;

import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NamedFileClass extends AbstractDynamicFormatFileClass<NamedFileData> {

    @Override
    protected String getFileSID() {
        return "NAMEDFILE";
    }

    @Override
    public NamedFileData getDefaultValue() {
        return NamedFileData.EMPTY;
    }

    @Override
    public Class getReportJavaClass() {
        return NamedFileData.class;
    }

    public static NamedFileClass instance = new NamedFileClass(false, false);

    private NamedFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof NamedFileClass ? this : null;
    }

    @Override
    public byte getTypeID() {
        return DataType.NAMEDFILE;
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if (typeFrom instanceof DynamicFormatFileClass) {
            return "cast_dynamic_file_to_named_file(" + value + ", null)";
        } else if(typeFrom instanceof NamedFileClass)
            return value;

        Result<String> rExtension = new Result<>();
        String castValue = StaticFormatFileClass.getCastToStatic(typeFrom, value, rExtension);
        if(castValue != null)
            return "cast_static_file_to_named_file(" + value + ", null, '" + rExtension.result + "')";

        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }

    @Override
    protected NamedFileData parseHTTPNotNull(FileData b, String charsetName, String fileName) {
        return ExternalRequest.getNamedFile(b, fileName);
    }

    @Override
    protected NamedFileData writePropNotNull(RawFileData value, String extension, String charset) {
        return new NamedFileData(new FileData(value, extension));
    }

    @Override
    public FileData readPropNotNull(NamedFileData value, String charset) {
        return value.getFileData();
    }

    @Override
    protected FileData formatHTTPNotNull(NamedFileData b, Charset charset, Result<String> fileName) {
        fileName.set(b.getName());
        return b.getFileData();
    }

    @Override
    protected NamedFileData readBytes(byte[] bytes) {
        return new NamedFileData(bytes);
    }

    @Override
    public NamedFileData read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        byte[] result = set.getBytes(name);
        if(result != null)
            return new NamedFileData(result);
        return null;
    }

    @Override
    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, value != null ? ((NamedFileData) value).getBytes() : null);
    }

    @Override
    public int getSize(NamedFileData value) {
        return value.getLength();
    }

    @Override
    protected NamedFileData getValue(RawFileData data) {
        return new NamedFileData(new FileData(data, "dat"));
    }

    @Override
    public String getCastToStatic(String value) {
        return "cast_named_file_to_static_file(" + value + ")";
    }

    @Override
    protected RawFileData getRawFileData(NamedFileData value) {
        return value.getRawFile();
    }
}