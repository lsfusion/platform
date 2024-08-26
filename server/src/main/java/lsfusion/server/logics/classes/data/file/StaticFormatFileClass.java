package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class StaticFormatFileClass extends FileClass<RawFileData> {

    public String getExtension(RawFileData file) {
        return getExtension();
    }

    protected StaticFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public abstract String getExtension();

    public FileData getFileData(RawFileData file) {
        return new FileData(file, getExtension(file));
    }

    public RawFileData getDefaultValue() {
        return RawFileData.EMPTY;
    }

    @Override
    public LA getDefaultOpenAction(BaseLogicsModule baseLM) {
        return baseLM.openRawFile;
    }

    public Class getReportJavaClass() {
        return RawFileData.class;
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, CastType castType) {
        if (typeFrom instanceof StringClass)
            return "cast_string_to_file(" + value + ")";
        if (typeFrom instanceof FileClass)
            return ((FileClass)typeFrom).getCastToStatic(value);
        if (typeFrom instanceof JSONClass)
            return "cast_json_to_static_file(" + value + ")";
        if (typeFrom instanceof JSONTextClass)
            return "cast_json_text_to_static_file(" + value + ")";

        return super.getCast(value, syntax, typeEnv, typeFrom, castType);
    }

    @Override
    protected RawFileData parseHTTPNotNull(FileData b, String charsetName) {
        return b.getRawFile();
    }

    @Override
    protected FileData formatHTTPNotNull(RawFileData b, Charset charset) {
        return getFileData(b);
    }

    @Override
    protected RawFileData writePropNotNull(RawFileData value, String extension, String charset) {
        return value;
    }

    @Override
    public RawFileData readPropNotNull(RawFileData value, String charset) {
        return value;
    }

    @Override
    public DataClass getCompatible(DataClass compClass, boolean or) {
        if(!(compClass instanceof StaticFormatFileClass))
            return null;

        StaticFormatFileClass staticFileClass = (StaticFormatFileClass)compClass;
        if(!(multiple == staticFileClass.multiple && storeName == staticFileClass.storeName))
            return null;
        
        if(equals(compClass))
            return this;

//        ImSet<String> mergedExtensions = getExtensions().merge(staticFileClass.getExtensions());
        return CustomStaticFormatFileClass.get(multiple, storeName);
    }

    public RawFileData read(Object value) {
        if(value instanceof byte[])
            return new RawFileData((byte[]) value);
        return (RawFileData) value;
    }

    public RawFileData read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        byte[] result = set.getBytes(name);
        if(result != null)
            return new RawFileData(result);
        return null;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, value != null ? ((RawFileData) value).getBytes() : null);
    }

    @Override
    public int getSize(RawFileData value) {
        return value.getLength();
    }

    @Override
    protected RawFileData getValue(RawFileData data) {
        return data;
    }

    @Override
    protected RawFileData getRawFileData(RawFileData value) {
        return value;
    }

    @Override
    public String getCastToStatic(String value) {
        return value;
    }

    public abstract FormIntegrationType getIntegrationType();
}
