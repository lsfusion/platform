package platform.server.data.type;

import platform.base.ExtInt;
import platform.base.col.interfaces.immutable.ImList;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.SQLSession;
import platform.server.data.query.TypeEnvironment;
import platform.server.data.sql.SQLSyntax;
import platform.server.form.view.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public interface Type<T> extends ClassReader<T> {

    public static interface Getter<K> {
        Type getType(K key);
    }

    String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv);

    String getDB(SQLSyntax syntax, TypeEnvironment typeEnv);
    int getSQL(SQLSyntax syntax);

    boolean isSafeString(Object value);
    boolean isSafeType(Object value);
    String getString(Object value, SQLSyntax syntax);

    void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException;
    void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException;

    Format getReportFormat();
    int getMinimumWidth();
    int getPreferredWidth();
    int getMaximumWidth();
    boolean fillReportDrawField(ReportDrawField reportField);

    Type getCompatible(Type type);

    ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass) throws SQLException;

    ImList<AndClassSet> getUniversal(BaseClass baseClass);

    ExtInt getCharLength();

    T parseString(String s) throws ParseException;
    
    AndClassSet getBaseClassSet(BaseClass baseClass);

    String getSID();
}
