package lsfusion.server.logics.classes.data.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.language.ScriptedStringUtils;
import lsfusion.server.logics.classes.data.ByteArrayClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.net.util.Base64;

import java.io.*;


public abstract class FileClass<T> extends FileBasedClass<T> {

    public final boolean multiple;
    public boolean storeName;

    protected FileClass(boolean multiple, boolean storeName) {
        super(LocalizedString.create("{classes.file}"));

        this.multiple = multiple;
        this.storeName = storeName;
    }

    @Override
    public DBType getDBType() {
        return ByteArrayClass.instance;
    }
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlBinary";
    }

    public String getDotNetRead(String reader) {
        throw new UnsupportedOperationException();
    }

    public String getDotNetWrite(String writer, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBaseDotNetSize() {
        throw new UnsupportedOperationException();
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getByteArraySQL();
    }

    @Override
    public ExtInt getCharLength() {
        return ExtInt.UNLIMITED;
    }

    @Override
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.CENTER;
    }

    protected abstract String getFileSID();

    @Override
    public String getSID() {
        return getFileSID() + (multiple ? "_Multiple" : "") + (storeName ? "_StoreName" : "");
    }

    @Override
    public String getCanonicalName() {
        return getFileSID();
    }

    @Override
    public boolean calculateStat() {
        return false;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
        outStream.writeBoolean(storeName);
    }

    public String formatString(T value) {
        return value != null ? Base64.encodeBase64StringUnChunked(getRawFileData(value).getBytes()) : null;
    }

    public T parseString(String s) {
        return getValue(new RawFileData(Base64.decodeBase64(s)));
    }

    public abstract String getCastToStatic(String value);

    public String getCastToConvert(boolean needImage, String value, SQLSyntax syntax) {
        String stringConcatenate = syntax.getStringConcatenate();
        return "'" + ScriptedStringUtils.wrapFile((needImage ? "1" : "0") + serializeString(), "'" + stringConcatenate + getEncode(value, syntax) + stringConcatenate + "'") + "'";
    }

    @Override
    public String formatStringSource(String valueSource, SQLSyntax syntax) {
        return getEncode(valueSource, syntax);
    }

    private String getEncode(String valueSource, SQLSyntax syntax) {
        return "encode(" + getCastToStatic(valueSource) + ", 'base64')";
    }

    @Override
    public String formatUI(T object) {
        if(object == null)
            return null;
        return getRawFileData(object).convertString();
    }

    protected abstract RawFileData getRawFileData(T value);
    protected abstract T getValue(RawFileData data);


    public String serializeString() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serialize(new DataOutputStream(outStream));
            return IOUtils.serializeStream(outStream);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T> FileClass<T> deserializeString(String image) throws IOException {
        DataInputStream dataStream = new DataInputStream(IOUtils.deserializeStream(image));
        return (FileClass) TypeSerializer.deserializeDataClass(dataStream);
    }
}
