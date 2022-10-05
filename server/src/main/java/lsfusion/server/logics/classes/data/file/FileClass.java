package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;


public abstract class FileClass<T> extends DataClass<T> {

    public final boolean multiple;
    public boolean storeName;

    protected FileClass(boolean multiple, boolean storeName) {
        super(LocalizedString.create("{classes.file}"));

        this.multiple = multiple;
        this.storeName = storeName;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getByteArrayType();
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
    public FlexAlignment getValueAlignment() {
        return FlexAlignment.CENTER;
    }

    protected abstract String getFileSID();

    public String getSID() {
        return getFileSID() + (multiple ? "_Multiple" : "") + (storeName ? "_StoreName" : "");
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

    @Override
    public T parseHTTP(Object o, Charset charset) throws ParseException {
        if(o instanceof String)
            return super.parseHTTP(o, charset);

        if (((FileData) o).getExtension().equals("null"))
            return null;
        return parseHTTPNotNull((FileData) o);
    }

    @Override
    public Object formatHTTP(T value, Charset charset) {
        if(charset != null)
            return super.formatHTTP(value, charset);

        if(value == null) 
            return new FileData(RawFileData.EMPTY, "null");
        return formatHTTPNotNull(value);
    }

    protected abstract T parseHTTPNotNull(FileData b);

    protected abstract FileData formatHTTPNotNull(T b);
}
