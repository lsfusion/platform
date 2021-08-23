package lsfusion.server.logics.classes.data.file;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import org.apache.commons.net.util.Base64;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class StaticFormatFileClass extends FileClass<RawFileData> {

    public abstract String getOpenExtension(RawFileData file);

    protected StaticFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public String getExtension() {
        return null;
    }

    public RawFileData getDefaultValue() {
        return RawFileData.EMPTY;
    }

    @Override
    public LA getDefaultOpenAction(BusinessLogics BL) {
        return BL.LM.openRawFile;
    }

    public Class getReportJavaClass() {
        return RawFileData.class;
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if (typeFrom instanceof StringClass) {
            return "cast_string_to_file(" + value + ")";
        } else if (typeFrom instanceof DynamicFormatFileClass) {
            return "cast_from_custom_file(" + value + ")";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    protected RawFileData parseHTTPNotNull(FileData b) {
        return b.getRawFile();
    }

    @Override
    protected FileData formatHTTPNotNull(RawFileData b) {
        return new FileData(b, getOpenExtension(b));
    }
    
    protected ImSet<String> getExtensions() {
        return SetFact.singleton(getExtension());
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

    public RawFileData parseString(String s) {
        return new RawFileData(Base64.decodeBase64(s));
    }

    public String formatString(RawFileData value) {
        return value != null ? Base64.encodeBase64StringUnChunked(value.getBytes()) : null;
    }

    public abstract FormIntegrationType getIntegrationType();
}
