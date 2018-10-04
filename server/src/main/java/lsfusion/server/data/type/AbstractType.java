package lsfusion.server.data.type;

import com.hexiong.jdbf.JDBFException;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.logics.property.actions.integration.exporting.plain.dbf.OverJDBField;
import net.iryndin.jdbf.core.DbfRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractType<T> extends AbstractReader<T> implements Type<T> {

    public boolean isSafeType() {
        return true;
    }

    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv) {
        return getCast(value, syntax, typeEnv, null);
    }
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        return "CAST(" + value + " AS " + getDB(syntax, typeEnv) + ")";
    }

    // CAST который возвращает NULL, если не может этого сделать 
    public String getSafeCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        if(hasSafeCast()) {
            typeEnv.addNeedSafeCast(this);
            return syntax.getSafeCastNameFnc(this) + "(" + value + ")";
        }
        return getCast(value, syntax, typeEnv, typeFrom);
    }
    
    public boolean hasSafeCast() {
        return false;
    }

    protected abstract void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException;
    public void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax) throws SQLException {
        writeParam(statement, num.get(), value, syntax);
    }
    public void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax) throws SQLException {
        statement.setNull(num.get(), getSQL(syntax));
    }

    public String getParamFunctionDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return getDB(syntax, typeEnv);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(getSID());
    }

    protected abstract int getBaseDotNetSize();

    @Override
    public int getDotNetSize() {
        return getBaseDotNetSize() + 1; // для boolean
    }

    public boolean useIndexedJoin() {
        return false;
    }

    @Override
    public boolean isFlex() {
        return false;
    }

    protected static boolean isParseNullValue(String value) {
        return value.equals("");
    }

    protected static String getParseNullValue() {
        return "";
    }

    public static Type getUnknownTypeNull() { // хак для общения нетипизированными параметрами
        return IntegerClass.instance;
    }

    @Override
    public T parseHTTP(Object o, Charset charset) throws ParseException {
        String s = (String) o;
        if(isParseNullValue(s))
            return null;
        return parseString(s);
    }

    @Override
    public Object formatHTTP(T value, Charset charset) {
        if(value == null)
            return getParseNullValue();
        return formatString(value);
    }

    public T parseNullableString(String string) throws ParseException {
        if(string == null)
            return null;
        return parseString(string);
    }
    @Override
    public T parseDBF(DbfRecord dbfRecord, String fieldName, String charset) throws ParseException, java.text.ParseException {
        return parseNullableString(dbfRecord.getString(fieldName, charset));
    }
    @Override
    public T parseJSON(JSONObject object, String key) throws ParseException, JSONException {
        Object o = object.get(key);
        if(o == JSONObject.NULL)
            o = null;
        return parseNullableString((String)o);
    }
    @Override
    public T parseXML(String value) throws ParseException {
        return parseNullableString(value);
    }
    @Override
    public T parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        String cellValue;
        switch (formulaValue.getCellTypeEnum()) {
            case STRING:
                cellValue = formulaValue.getStringValue();
                break;
            case NUMERIC:
                cellValue = new BigDecimal(formulaValue.getNumberValue()).toPlainString();
                break;        
            default:
                cellValue = formulaValue.formatAsString();
        }
        return parseNullableString(cellValue);
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return new OverJDBField(fieldName, 'C', 253, 0);
    }
    @Override
    public Object formatJSON(T object) {
        return formatString(object);
    }

    @Override
    public String formatXML(T object) {
        return formatString(object);
    }

    protected T readDBF(Object object) {
        return read(object);
    }
    protected T readJSON(Object object) {
        return read(object);
    }
    protected T readXLS(Object object) {
        return read(object);
    }
}
