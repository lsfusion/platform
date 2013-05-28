package platform.server.logics.property;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.ObjectValueClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Table;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.PropertyChanges;

// первично свойство, соответствующее полю хранящему значение класса
// строго системное свойство, в логике предполагается использование ObjectClassProperty
public class ClassDataProperty extends CalcProperty<ClassPropertyInterface> implements ClassField {

    public final ObjectValueClassSet set;

    public ClassDataProperty(String sID, String caption, ObjectValueClassSet set) {
        super(sID, caption, SetFact.singletonOrder(new ClassPropertyInterface(0, set.getOr().getCommonClass())));
        this.set = set;
    }

    public boolean isStored() {
        return true;
    }

    @Override
    public boolean isEnabledSingleApply() {  // нельзя single Apply'ить потому что в DataObject'ах и таблицах зависают конкретные классы
        return false;
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        throw new RuntimeException("should not be");
    }

    protected ClassWhere<Object> getClassValueWhere(ClassType type) {
        return new ClassWhere<Object>(MapFact.<Object, AndClassSet>toMap(interfaces.single(), set, "value", set.getBaseClass().objectClass));
    }

    public Table.Join.Expr getStoredExpr(Expr expr) {
        return (Table.Join.Expr) getStoredExpr(MapFact.singleton(interfaces.single(), expr));
    }

    public ObjectValueClassSet getSet() {
        return set;
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("should not be");
    }
}
