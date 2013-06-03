package lsfusion.server.data.type;

import lsfusion.base.ExtInt;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.form.view.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public interface Type<T> extends ClassReader<T> {

    public static interface Getter<K> {
        Type getType(K key);
    }

    public Object getInfiniteValue(boolean min);

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
