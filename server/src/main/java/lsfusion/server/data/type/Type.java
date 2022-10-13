package lsfusion.server.data.type;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.ExtInt;
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

public interface Type<T> extends ClassReader<T>, FunctionType {

    boolean useIndexedJoin();

    interface Getter<K> {
        Type getType(K key);
    }

    T getInfiniteValue(boolean min);

    String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv); // как правило нужен, чтобы указать СУБД класс, а не реально прокастить 
    String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom);
    String getSafeCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom, boolean isArith);

    String getDB(SQLSyntax syntax, TypeEnvironment typeEnv);
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

    ExtInt getCharLength();
    
    FlexAlignment getValueAlignment();

    T parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws ParseException, java.text.ParseException, IOException;
    T parseJSON(Object value) throws ParseException, JSONException;
    T parseCSV(String value) throws ParseException;
    T parseXML(String value) throws ParseException;
    T parseXLS(Cell cell, CellValue formulaValue) throws ParseException;

    OverJDBField formatDBF(String fieldName) throws JDBFException;
    Object formatJSON(T object);
    String formatJSONSource(String valueSource, SQLSyntax syntax); // should correspond formatJSON
    String getJSONType();
    String formatCSV(T object);
    String formatXML(T object);
    void formatXLS(T object, Cell cell, ExportXLSWriter.Styles styles);

    T parseNullableString(String s, boolean emptyIsNull) throws ParseException; // s - can be null
    
    T parseString(String s) throws ParseException; // s - not null (файлы decode'ся base64)

    String formatString(T value); // возвращает null если передали null (файлы encode'ся base64)
    String formatStringSource(String valueSource, SQLSyntax syntax); // should correspond formatString

    T parseHTTP(Object o, Charset charset) throws ParseException; // o - String or FileData, o - not null, null'ы decode'ся в зависимости от типа

    Object formatHTTP(T value, Charset charset); // returns String or FileData (не null), null'ы encode'ит в зависимости от типа

    T writeProp(RawFileData value, String extension);

    RawFileData readProp(T value, String charset);

    AndClassSet getBaseClassSet(BaseClass baseClass);

    String getSID();

    T read(Object value);

    default boolean useInputTag(boolean isPanel) {
        return false;
    }
    default boolean hasToolbar(boolean isInputPanel) {
        if(isInputPanel)
            return !Settings.get().isNoToolbarForInputTagInPanel();
        return true;
    }
}
