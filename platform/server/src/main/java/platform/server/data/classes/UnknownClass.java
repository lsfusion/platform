package platform.server.data.classes;

import platform.server.data.classes.where.*;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.types.ObjectType;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.session.SQLSession;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

public class UnknownClass implements ConcreteObjectClass {

    public String toString() {
        return "Неизвестный";
    }

    public final BaseClass baseClass;
    
    UnknownClass(BaseClass iBaseClass) {
        baseClass = iBaseClass;
    }

    public void getDiffSet(ConcreteObjectClass diffClass, Collection<CustomClass> addClasses, Collection<CustomClass> removeClasses) {
        if(diffClass instanceof CustomClass) // все удаляются
            ((CustomClass)diffClass).fillParents(removeClasses);
    }

    public void saveClassChanges(SQLSession session, DataObject value) throws SQLException {
        session.deleteKeyRecords(baseClass.table,Collections.singletonMap(baseClass.table.key,(Integer)value.object));
    }

    public boolean inSet(AndClassSet set) {
        return set instanceof UnknownClass && equals(set);
    }

    public boolean containsAll(AndClassSet node) {
        return inSet(node);
    }

    public OrClassSet getOr() {
        return new OrObjectClassSet();
    }

    public SourceExpr getIDExpr() {
        return CaseExpr.NULL;
    }

    public Type getType() {
        return ObjectType.instance;
    }

    public String getWhereString(String source) {
        return source + " IS NULL";
    }

    public String getNotWhereString(String source) {
        return source + " IS NOT NULL";
    }

    public ObjectClassSet and(AndClassSet node) {
        return inSet(node)?this:UpClassSet.FALSE;
    }

    public boolean isEmpty() {
        return false;
    }

    public BaseClass getBaseClass() {
        return baseClass;
    }
}
