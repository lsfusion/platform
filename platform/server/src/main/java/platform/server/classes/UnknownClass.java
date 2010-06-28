package platform.server.classes;

import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

public class UnknownClass implements ConcreteObjectClass {

    public String toString() {
        return "Неизвестный";
    }

    public final BaseClass baseClass;
    
    UnknownClass(BaseClass baseClass) {
        this.baseClass = baseClass;
    }

    public void getDiffSet(ConcreteObjectClass diffClass, Collection<CustomClass> addClasses, Collection<CustomClass> removeClasses) {
        if(diffClass instanceof CustomClass) // все удаляются
            ((CustomClass)diffClass).fillParents(removeClasses);
    }

    public void saveClassChanges(SQLSession session, DataObject value) throws SQLException {
        session.deleteKeyRecords(baseClass.table,Collections.singletonMap(baseClass.table.key,value.object));
    }

    public boolean inSet(AndClassSet set) {
        return set.containsAll(this);
    }

    public boolean containsAll(AndClassSet node) {
        return node instanceof UnknownClass && equals(node);
    }

    public OrObjectClassSet getOr() {
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
        return ConcreteCustomClass.and(this,node);
    }

    public AndClassSet or(AndClassSet node) {
        return ConcreteCustomClass.or(this,node);
    }

    public boolean isEmpty() {
        return false;
    }

    public BaseClass getBaseClass() {
        return baseClass;
    }

    public AndClassSet getKeepClass() {
        return this;
    }
}
