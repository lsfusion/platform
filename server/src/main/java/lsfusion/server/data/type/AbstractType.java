package lsfusion.server.data.type;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.JDBFException;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.reader.AbstractReader;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import lsfusion.server.logics.form.stat.struct.imports.plain.dbf.CustomDbfRecord;
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
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractType<T> extends AbstractReader<T> implements Type<T> {

    public boolean isSafeType() {
        return true;
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

    @Override
    public boolean isFlex() {
        return false;
    }

    @Override
    public FlexAlignment getValueAlignmentHorz() {
        return FlexAlignment.START;
    }

    @Override
    public FlexAlignment getValueAlignmentVert() {
        return FlexAlignment.CENTER;
    }

    @Override
    public String getValueOverflowHorz() {
        return "clip";
    }

    @Override
    public boolean getValueShrinkHorz() {
        return false;
    }

    @Override
    public boolean getValueShrinkVert() {
        return false;
    }

    protected static boolean isParseNullValue(String value) {
        return value.equals("");
    }

    protected static String getParseNullValue() {
        return "";
    }

    public static ValueClass getUnknownClassNull() {
        return IntegerClass.instance;
    }
    public static Type getUnknownTypeNull() { // хак для общения нетипизированными параметрами
        return IntegerClass.instance;
    }

    @Override
    public T parseHTTP(ExternalRequest.Param param) throws ParseException {
        Object value = param.value;
        if(value instanceof FileData)
            value = ExternalUtils.encodeFileData((FileData) value, param.charsetName);

        String s = (String) value;
        if(isParseNullValue(s))
            return null;
        return parseString(s);
    }

    @Override
    public ExternalRequest.Result formatHTTP(T value, Charset charset, boolean needFileName) {
        return new ExternalRequest.Result(formatNullableString(value, true));
    }

    @Override
    public T parseFile(NamedFileData value, String charset) {
        if(value == null)
            return null;
        return writePropNotNull(value, charset);
    }

    protected T writePropNotNull(NamedFileData value, String charset) {
        try {
            return parseString(value.getRawFile().getString(charset));
        } catch (ParseException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public NamedFileData formatFile(T value, String charset) {
        if(value == null)
            return null;
        return readPropNotNull(value, charset);
    }

    public NamedFileData readPropNotNull(T value, String charset) {
        return new NamedFileData(new RawFileData(formatString(value), charset));
    }

    public T parseNullableString(String string, boolean emptyIsNull) throws ParseException {
        if(string == null || (emptyIsNull && string.isEmpty()))
            return null;
        return parseString(string);
    }

    protected String formatNullableString(T object, boolean nullIsEmpty) {
        if(nullIsEmpty && object == null)
            return getParseNullValue();
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
            case ERROR:
                cellValue = null;
                break;
            default:
                cellValue = formulaValue.formatAsString();
        }
        return parseNullableString(cellValue, true);
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return OverJDBField.createField(fieldName, 'C', 253, 0);
    }
    @Override
    public Object formatJSON(T object) {
        return formatNullableString(object, false); // json supports nulls
    }
    @Override
    public String formatJSONSource(String valueSource, SQLSyntax syntax) {
        return formatStringSource(valueSource, syntax);
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
    public String formatConnectionString(T object) {
        return formatNullableString(object, true);
    }

    @Override
    public T parseUI(String value, String pattern) throws ParseException {
        return parseString(value);
    }

    @Override
    public String formatUI(T object, String pattern) {
        return formatString(object);
    }

    @Override
    public void formatXLS(T object, Cell cell, ExportXLSWriter.Styles styles) {
        String formatted = formatNullableString(object, false); // xls supports nulls
        if(formatted != null)
            cell.setCellValue(formatted);
    }

    public T readResult(Object object) {
        return read(object);
    }
    public T readCast(Object value, Type typeFrom) {
        return read(value);
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

    public T read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return readResult(set.getObject(name));
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    @Override
    public String formatString(T value) {
        return value == null ? null : value.toString();
    }

    @Override
    public String formatStringSource(String valueSource, SQLSyntax syntax) {
        return valueSource;
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        throw new UnsupportedOperationException();
    }
}
