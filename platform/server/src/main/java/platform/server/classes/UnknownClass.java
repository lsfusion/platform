package platform.server.classes;

import platform.base.ImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.caches.IdentityLazy;
import platform.server.caches.IdentityStrongLazy;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ActionProperty;

public class UnknownClass extends ImmutableObject implements ConcreteObjectClass {

    public String toString() {
        return ServerResourceBundle.getString("classes.unknown");
    }

    public final BaseClass baseClass;
    
    public UnknownClass(BaseClass baseClass) {
        this.baseClass = baseClass;
    }

    public void getDiffSet(ConcreteObjectClass diffClass, MSet<CustomClass> mAddClasses, MSet<CustomClass> mRemoveClasses) {
        if(diffClass instanceof CustomClass) // все удаляются
            ((CustomClass)diffClass).fillParents(mRemoveClasses);
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

    public ObjectValue getClassObject() {
        return NullValue.instance;
    }

    public Type getType() {
        return ObjectType.instance;
    }

    public Stat getTypeStat() {
        return Stat.MAX;
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

    public AndClassSet[] getAnd() {
        return new AndClassSet[]{this};
    }

    @IdentityStrongLazy // для ID
    public ActionProperty getChangeClassAction() {
        return CustomClass.getChangeClassAction(this);
    }
}
