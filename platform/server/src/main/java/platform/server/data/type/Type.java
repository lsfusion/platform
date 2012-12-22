package platform.server.data.type;

import platform.base.col.interfaces.immutable.ImList;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.form.view.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.List;

public interface Type<T> extends ClassReader<T> {

    public static interface Getter<K> {
        Type getType(K key);
    }

    String getCast(String value, SQLSyntax syntax, boolean needLength);
    String getBinaryCast(String value, SQLSyntax syntax, boolean needLength);
    
    String getDB(SQLSyntax syntax);
    int getSQL(SQLSyntax syntax);

    boolean isSafeString(Object value);
    boolean isSafeType(Object value);
    String getString(Object value, SQLSyntax syntax);

    void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException;

    Format getReportFormat();
    int getMinimumWidth();
    int getPreferredWidth();
    int getMaximumWidth();
    boolean fillReportDrawField(ReportDrawField reportField);

    Type getCompatible(Type type);

    ConcreteClass getDataClass(Object value, SQLSession session, BaseClass baseClass) throws SQLException;

    ImList<AndClassSet> getUniversal(BaseClass baseClass);

    int getBinaryLength(boolean charBinary);
    ConcreteClass getBinaryClass(byte[] value, SQLSession session, BaseClass baseClass) throws SQLException;

    T parseString(String s) throws ParseException;
    
    AndClassSet getBaseClassSet(BaseClass baseClass);

    Stat getDefaultStat();
}
