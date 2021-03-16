package lsfusion.server.data.type;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.file.FileData;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.reader.AbstractReader;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import lsfusion.server.logics.form.stat.struct.imports.plain.dbf.CustomDbfRecord;
import lsfusion.server.physics.admin.Settings;
import net.iryndin.jdbf.core.DbfFieldTypeEnum;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
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
    public String getSafeCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, boolean isArith) {
        if(hasSafeCast()) {
            boolean isInt = isArith || typeFrom instanceof IntegralClass;
            if(!(isInt && Settings.get().getSafeCastIntType() == 2)) {
                typeEnv.addNeedSafeCast(this, isInt);
                return syntax.getSafeCastNameFnc(this, isInt) + "(" + value + ")";
            }
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

    @Override
    public FlexAlignment getValueAlignment() {
        return FlexAlignment.START;
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
        String s = o instanceof FileData ? new String(((FileData) o).getRawFile().getBytes(), charset) :  (String) o;
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

    public T parseNullableString(String string, boolean emptyIsNull) throws ParseException {
        if(string == null || (emptyIsNull && string.isEmpty()))
            return null;
        return parseString(string);
    }

    protected String formatNullableString(T object, boolean nullIsEmpty) {
        if(nullIsEmpty && object == null)
            return "";            
        return formatString(object);
    }

    @Override
    public T parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws ParseException, java.text.ParseException, IOException {
        String string;
        if(dbfRecord.getField(fieldName).getType() == DbfFieldTypeEnum.Memo)
            string = dbfRecord.getMemoAsString(fieldName, Charset.forName(charset));
        else
            string = dbfRecord.getString(fieldName, charset);
        return parseNullableString(string, false); // dbf supports nulls
    }
    @Override
    public T parseJSON(Object value) throws ParseException, JSONException {
        return parseNullableString(value != null && value != JSONObject.NULL ? value.toString() : null, false); // json supports nulls
    }
    @Override
    public T parseCSV(String value) throws ParseException {
        return parseNullableString(value, true);
    }
    @Override
    public T parseXML(String value) throws ParseException {
        return parseNullableString(value, true);
    }
    @Override
    public T parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        String cellValue;
        switch (formulaValue.getCellType()) {
            case STRING:
                cellValue = formulaValue.getStringValue();
                break;
            case NUMERIC:
                if(DateUtil.isCellDateFormatted(cell)) {
                    cellValue = DateClass.instance.formatString(cell.getLocalDateTimeCellValue().toLocalDate());
                } else {
                    cellValue = BigDecimal.valueOf(formulaValue.getNumberValue()).stripTrailingZeros().toPlainString();
                }
                break;        
            default:
                cellValue = formulaValue.formatAsString();
        }
        return parseNullableString(cellValue, true);
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return new OverJDBField(fieldName, 'C', 253, 0);
    }
    @Override
    public Object formatJSON(T object) {
        return formatNullableString(object, false); // json supports nulls
    }
    @Override
    public String getJSONType() {
        return "string";
    }
    @Override
    public String formatCSV(T object) {
        return formatNullableString(object, true);
    }

    @Override
    public String formatXML(T object) {
        return formatNullableString(object, true);
    }

    @Override
    public void formatXLS(T object, Cell cell, ExportXLSWriter.Styles styles) {
        String formatted = formatNullableString(object, false); // xls supports nulls
        if(formatted != null)
            cell.setCellValue(formatted);
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
