package lsfusion.server.classes.link;

import lsfusion.base.ExtInt;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public abstract class LinkClass extends DataClass<String> {

    public final boolean multiple;

    protected LinkClass(boolean multiple) {
        super(ServerResourceBundle.getString("classes.link"));

        this.multiple = multiple;
    }

    public Object getDefaultValue() {
        return "";
    }

    public Class getReportJavaClass() {
        return String.class;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getTextType();
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlString";
    }
    public String getDotNetRead(String reader) {
        return reader + ".ReadString()";
    }
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    @Override
    public int getBaseDotNetSize() {
        return 400;
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getTextSQL();
    }

    public boolean isSafeString(Object value) {
        return !value.toString().contains("'") && !value.toString().contains("\\");
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public String read(Object value) {
        return (String) value;
    }

    public String read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return read(set.getString(name));
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setString(num, (String) value);
    }

    @Override
    public ExtInt getCharLength() {
        return ExtInt.UNLIMITED;
    }

    @Override
    public int getSize(String value) {
        return value.length(); //длина ссылки, а не размер файла
    }

    public String parseString(String s) throws ParseException {
        return s;
    }

    protected abstract String getFileSID();

    public String getSID() {
        return getFileSID() + (multiple ? "_Multiple" : "");
    }

    @Override
    public boolean calculateStat() {
        return false;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
    }

    public List<URI> getFiles(String value) {
        List<URI> files = new ArrayList<>();
        if (value != null) {
            for (String file : Arrays.asList(value.split(";"))) {
                files.add(getURI(file));
            }
        }
        return files;
    }

    public List<URI> getFiles(Object value) {
        return getFiles((String) value);
    }

    private URI getURI(String value) {
        URI result;
        try {
            result = new URI(URIUtil.encodeQuery(value));
        } catch (URISyntaxException | URIException e) {
            result = null;
        }
        return result;
    }

    @Override
    public boolean fixedSize() {
        return false;
    }
}