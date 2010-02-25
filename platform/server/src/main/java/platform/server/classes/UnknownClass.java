package platform.server.classes;

import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.classes.sets.*;

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
        session.deleteKeyRecords(baseClass.table,Collections.singletonMap(baseClass.table.key,value.object));
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

    public Expr getIDExpr() {
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
        return inSet(node)?this: UpClassSet.FALSE;
    }

    public boolean isEmpty() {
        return false;
    }

    public BaseClass getBaseClass() {
        return baseClass;
    }
}
