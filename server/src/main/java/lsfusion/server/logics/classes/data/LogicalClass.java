package lsfusion.server.logics.classes.data;

import com.hexiong.jdbf.JDBFException;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import lsfusion.server.logics.form.stat.struct.imports.plain.dbf.CustomDbfRecord;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LogicalClass extends DataClass<Boolean> {

    public static final LogicalClass instance = new LogicalClass(false);
    
    public static final LogicalClass threeStateInstance = new LogicalClass(true);

    static {
        DataClass.storeClass(instance);
        DataClass.storeClass(threeStateInstance);
    }

    public final boolean threeState;
    
    private LogicalClass(boolean threeState) { 
        super(LocalizedString.create("{classes.logical}"));
        this.threeState = threeState;
    }

    public int getReportPreferredWidth() { return 50; }

    public Class getReportJavaClass() {
        return Boolean.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = HorizontalTextAlignEnum.CENTER;
    }

    public byte getTypeID() {
        return threeState ? DataType.TLOGICAL : DataType.LOGICAL;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof LogicalClass && threeState == ((LogicalClass) compClass).threeState ? this : null;
    }

    public Boolean getDefaultValue() {
        return true;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getBitType();
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlInt32";
    }

    public String getDotNetRead(String reader) {
        return reader + ".ReadInt32()";
    }
    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" + value + ");";
    }

    @Override
    public int getBaseDotNetSize() {
        return 4;
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getBitSQL();
    }

    public Boolean read(Object value) {
        if (threeState) {
            if (value instanceof Boolean)
                return (Boolean) value;
            else if (value != null)
                return value == (Integer) 1;
        } else {
            if (value instanceof Boolean)
                return (Boolean) value ? true : null;
            else if (value != null)
                return true;
        }
        return null;
    }

    @Override
    public boolean useInputTag(boolean isPanel) {
        return Settings.get().isUseInputTagForBoolean();
    }

    @Override
    public boolean hasToolbar(boolean isInputPanel) {
        return !Settings.get().isNoToolbarForBoolean();
    }

    @Override
    public Boolean read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return super.read(set, syntax, name);//set.getBoolean(name);
    }

    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        return syntax.getBitString((Boolean) value);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        assert (Boolean)value;
        statement.setByte(num, (byte) (threeState ? ((Boolean) value ? 1 : 0) : 1));
    }

/*    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        return syntax.getBitString((Boolean) value);
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setByte(num, (byte) ((Boolean)value?1:0));
    }
  */

    @Override
    public ExtInt getCharLength() {
        return new ExtInt(1);
    }

    @Override
    public FlexAlignment getValueAlignment() {
        return FlexAlignment.CENTER;
    }

    @Override
    public Boolean getInfiniteValue(boolean min) {
        return true;
    }

    public Boolean shiftValue(Boolean object, boolean back) {
        return object==null?true:null;
    }

    @Override
    public Boolean parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) {
        return readDBF(dbfRecord.getBoolean(fieldName));
    }
    @Override
    public Boolean parseJSON(Object value) throws JSONException {
        if(value == JSONObject.NULL)
            return null;
        return readJSON(value);
    }
    @Override
    public Boolean parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        if(formulaValue.getCellType().equals(CellType.BOOLEAN))
            return readXLS(formulaValue.getBooleanValue());
        return super.parseXLS(cell, formulaValue);
    }

    @Override
    public Object formatJSON(Boolean object) {
        return formatBoolean(object);
    }

    @Override
    public String getJSONType() {
        return "boolean";
    }

    @Override
    public void formatXLS(Boolean object, Cell cell, ExportXLSWriter.Styles styles) {
        Boolean result = formatBoolean(object);
        if(result != null) {
            cell.setCellValue(result);
        }
    }

    private Boolean formatBoolean(Boolean object) {
        if (threeState)
            return object;
        else
            return object != null && object ? true : null;
    }

    public Boolean parseString(String s) throws ParseException {
        if (s != null) {
            if (s.equalsIgnoreCase("true"))
                return true;
            else if (threeState && s.equalsIgnoreCase("false"))
                return false;
        }
        return null;
    }

    @Override
    public String formatString(Boolean value) {
        return value == null ? null : String.valueOf(value);
    }

    public String getSID() {
        return threeState ? "TBOOLEAN" : "BOOLEAN";
    }

    @Override
    public Stat getTypeStat() {
        return Stat.ONE;
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return OverJDBField.createField(fieldName, 'L', 1, 0);
    }

    public boolean calculateStat() {
        return false;
    }
}
