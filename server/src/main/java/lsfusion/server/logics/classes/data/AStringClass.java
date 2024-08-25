package lsfusion.server.logics.classes.data;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.BaseUtils;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.DBType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.Math.max;

public abstract class AStringClass extends TextBasedClass<String> {

    public final boolean blankPadded;
    public final boolean caseInsensitive;
    public final ExtInt length;

    protected AStringClass(LocalizedString caption, boolean blankPadded, ExtInt length, boolean caseInsensitive) {
        super(caption);
        this.blankPadded = blankPadded;
        this.length = length;
        this.caseInsensitive = caseInsensitive;
    }

    public int getReportMinimumWidth() {
        return 30;
    }

    public int getReportPreferredWidth() {
        if(length.isUnlimited())
            return 200;
        return Math.min(200, max(30, length.getValue() * 2));
    }

    private boolean isDBType() {
        return !caseInsensitive && getClass() == StringClass.class;
    }
    @Override
    public DBType getDBType() {
        if(isDBType()) // optimization
            return (StringClass) this;

        return StringClass.get(blankPadded, false, length);
    }

    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
        assert isDBType();
        boolean isUnlimited = length.isUnlimited();
        if(blankPadded) {
            if(isUnlimited)
                return syntax.getBPTextType();
            int lengthValue = length.getValue();
            return syntax.getStringType(lengthValue==0 ? 1 : lengthValue);
        }
        if(isUnlimited)
            return syntax.getTextType();
        int lengthValue = length.getValue();
        return syntax.getVarStringType(lengthValue==0? 1 : lengthValue);
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
        if(length.isUnlimited())
            return 400;
        return length.getValue() * 4 + 5;
    }

    public int getSQL(SQLSyntax syntax) {
        boolean isUnlimited = length.isUnlimited();
        if(blankPadded) {
            if(isUnlimited)
                return syntax.getBPTextSQL();
            return syntax.getStringSQL();
        }
        if(isUnlimited)
            return syntax.getTextSQL();
        return syntax.getVarStringSQL();
    }

    public boolean isSafeString(Object value) {
        if(value == null)
            return false;

        assert value instanceof String;
        String string = value.toString();
        return !string.contains("'") && !string.contains("\\");
    }

    public String read(Object value) {
        if (value == null) return null;

        if(blankPadded) {
            if(length.isUnlimited())
                return ((String)value);
//            return BaseUtils.padr((String) value, length.getValue());
            return BaseUtils.rtrim(BaseUtils.truncate((String) value, length.getValue()));
        }

        if(length.isUnlimited())
            return (String) value;
        return BaseUtils.truncate((String) value, length.getValue());
    }

    @Override
    public String read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return read(set.getString(name));
    }

    @Override
    public ExtInt getCharLength() {
        return length;
    }

    @Override
    public int getSize(String value) {
        assert length.isUnlimited();
        return value.length();
    }

    @Override
    public Stat getTypeStat() {
        if(length.isUnlimited())
            return new Stat(100, 400);
        return new Stat(100, length.getValue());
    }

    @Override
    public boolean fixedSize() {
        return false;
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        ExtInt charLength = getCharLength();
        return OverJDBField.createField(fieldName, 'C', Math.min(charLength.isUnlimited() ? Integer.MAX_VALUE : charLength.getValue(), 253), 0);
    }

    @Override
    public boolean isFlex() {
        return true;
    }

    @Override
    public Compare getDefaultCompare() {
        if(caseInsensitive || Settings.get().isDefaultCompareForStringContains())
            return Settings.get().isDefaultCompareSearchInsteadOfContains() ? Compare.MATCH : Compare.CONTAINS;

        return super.getDefaultCompare();
    }

    public Class getReportJavaClass() {
        return String.class;
    }

    public String getDefaultValue() {
        return "";
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    @Override
    public boolean isSafeType() { // при полиморфных функциях странно себя ведет без explicit cast'а
        return false;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setString(num, (String) value);
    }

    public String parseString(String s) {
        return s.replace("\u0000", "");
    }

}
