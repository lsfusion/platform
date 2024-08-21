package lsfusion.server.data.type;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import lsfusion.server.logics.form.stat.struct.imports.plain.dbf.CustomDbfRecord;
import lsfusion.server.physics.admin.Settings;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static java.lang.Math.pow;
import static java.lang.Math.round;

public interface Type<T> extends ClassReader<T>, FunctionType {

    interface Getter<K> {
        Type getType(K key);
    }

    T getInfiniteValue(boolean min);

    default boolean equalsDB(Type typeFrom) { // private
        return typeFrom != null && getDBType().equals(typeFrom.getDBType());
    }

    default boolean isCastNotNull(Type typeFrom) {
        return isCastNotNull(typeFrom, false);
    }
    default String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        return getCast(value, syntax, typeEnv, typeFrom, false);
    }

    default String getArithCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv) {
        return getCast(value, syntax, typeEnv, null, true);
    }
    default boolean isCastNotNull(Type typeFrom, boolean isArith) { // returns true if cast can return NULL for the non-NULL value
        return false;
    }
    default String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, boolean isArith) {
        if(typeFrom != null && equalsDB(typeFrom))
            return value;

        return getCast(value, syntax, typeEnv);
    }
    default String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "CAST(" + value + " AS " + getDB(syntax, typeEnv) + ")";
    }

    DBType getDBType();
    default String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return getDBType().getDBString(syntax, typeEnv);
    }
    String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv); // for ms sql
    String getDotNetRead(String reader); // for ms sql
    String getDotNetWrite(String writer, String value); // for ms sql
    int getDotNetSize(); // for ms sql
    int getSQL(SQLSyntax syntax);

    boolean isSafeString(Object value);
    boolean isSafeType();
    String getString(Object value, SQLSyntax syntax);

    void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax) throws SQLException;
    void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax) throws SQLException;

    int getReportMinimumWidth();
    int getReportPreferredWidth();

    default Compare getDefaultCompare() {
        return null;
    }

    void fillReportDrawField(ReportDrawField reportField);
    
    boolean isFlex();

    Type getCompatible(Type type);

    ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException;

    ImList<AndClassSet> getUniversal(BaseClass baseClass);

    Stat getTypeStat(boolean forJoin);

    default int getAverageCharLength() {
        ExtInt length = getCharLength();
        if(length.isUnlimited())
            return 15;

        int lengthValue = length.getValue();
        return lengthValue <= 12 ? Math.max(lengthValue, 1) : (int) round(12 + pow(lengthValue - 12, 0.7));
    }
    ExtInt getCharLength();

    String getValueAlignmentHorz();
    String getValueAlignmentVert();

    String getValueOverflowHorz();

    boolean getValueShrinkHorz();

    T parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws ParseException, java.text.ParseException, IOException;
    T parseJSON(Object value) throws ParseException, JSONException;
    T parseCSV(String value) throws ParseException;
    T parseXML(String value) throws ParseException;
    T parseXLS(Cell cell, CellValue formulaValue) throws ParseException;

    T parsePaste(String value) throws ParseException;

    OverJDBField formatDBF(String fieldName) throws JDBFException;
    Object formatJSON(T object);
    String formatJSONSource(String valueSource, SQLSyntax syntax); // should correspond formatJSON
    String getJSONType();
    String formatCSV(T object);
    String formatXML(T object);
    void formatXLS(T object, Cell cell, ExportXLSWriter.Styles styles);

    String formatMessage(T object);
    String formatEmail(T object); // inline'ing files as "html"

    T parseString(String s) throws ParseException; // s - not null (файлы decode'ся base64)

    String formatString(T value, boolean ui);
    default String formatString(T value) { // Returns null if passed null (files are base64 encoded)
        return formatString(value, false);
    }
    String formatStringSource(String valueSource, SQLSyntax syntax); // should correspond formatString

    T parseHTTP(ExternalRequest.Param param) throws ParseException; // param.value - String or FileData, param.value - not null, nulls are decoded depending on type

    Object formatHTTP(T value, Charset charset); // returns String or FileData (not null), null's encode'it depending on type

    T writeProp(RawFileData value, String extension, String charset);

    RawFileData readProp(T value, String charset);

    AndClassSet getBaseClassSet(BaseClass baseClass);

    String getSID();

    T read(Object value);

    default boolean useInputTag(boolean isPanel, boolean useBootstrap, Type changeType) {
        return false;
    }
    default boolean hasToolbar(boolean isInputPanel) {
        if(isInputPanel)
            return !Settings.get().isNoToolbarForInputTagInPanel();
        return true;
    }

    default String getInputType(FormInstanceContext context) {
        return "text";
    }
}
