package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
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
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if (typeFrom instanceof DynamicFormatFileClass) {
            return "cast_dynamic_file_to_named_file(" + value + ", null)";
        } else if (typeFrom instanceof StaticFormatFileClass) {
            String extension = ((StaticFormatFileClass) typeFrom).getExtension();
            return "cast_static_file_to_named_file(" + value + ", null, " + (extension != null ? ("'" + extension + "'") : "null") + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    protected NamedFileData parseHTTPNotNullString(String s, Charset charset) {
        return new NamedFileData(new FileData(s.getBytes(charset)), "file");
    }

    @Override
    protected NamedFileData parseHTTPNotNull(FileData b) {
        return new NamedFileData(b, "file");
    }

    @Override
    protected String formatHTTPNotNullString(NamedFileData value, Charset charset) {
        return value != null ? new String(value.getRawFile().getBytes(), charset) : null;
    }

    @Override
    protected NamedFileData writePropNotNull(RawFileData value, String extension) {
        return new NamedFileData(new FileData(value, extension), "file");
    }

    @Override
    public RawFileData readPropNotNull(NamedFileData value, String charset) {
        return value.getRawFile();
    }

    @Override
    protected FileData formatHTTPNotNull(NamedFileData b) {
        return new FileData(b.getRawFile(), b.getExtension());
    }

    @Override
    public NamedFileData read(Object value) {
        if(value instanceof byte[])
            return new NamedFileData((byte[]) value);
        return (NamedFileData) value;
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
    public NamedFileData parseString(String s) {
        return new NamedFileData(Base64.decodeBase64(s));
    }

    @Override
    public String formatString(NamedFileData value) {
        return value != null ? Base64.encodeBase64StringUnChunked(value.getBytes()) : null;
    }
}