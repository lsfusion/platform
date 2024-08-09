package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.ByteArrayClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.net.util.Base64;

import java.io.DataOutputStream;
import java.io.IOException;


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
    public String getValueAlignmentHorz() {
        return "center";
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

    public String formatString(T value, boolean ui) {
        return value != null ? Base64.encodeBase64StringUnChunked(getBytes(value)) : null;
    }
    protected abstract byte[] getBytes(T value);

    @Override
    public String formatStringSource(String valueSource, SQLSyntax syntax) {
        return "encode(" + valueSource + ", 'base64')";
    }
}
