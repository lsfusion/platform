package lsfusion.server.logics.property;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.Table;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.session.PropertyChanges;

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
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        throw new RuntimeException("should not be");
    }

    protected ClassWhere<Object> getClassValueWhere(ClassType type, PrevClasses prevSameClasses) {
        return new ClassWhere<Object>(MapFact.<Object, AndClassSet>toMap(interfaces.single(), set, "value", set.getBaseClass().objectClass));
    }

    public Table.Join.Expr getInconsistentExpr(Expr expr) {
        return getInconsistentExpr(MapFact.singleton(interfaces.single(), expr), set.getBaseClass());
    }

    public Table.Join.Expr getStoredExpr(Expr expr) {
        return (Table.Join.Expr) getStoredExpr(MapFact.singleton(interfaces.single(), expr));
    }

    public ObjectValueClassSet getObjectSet() {
        return set;
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("should not be");
    }
}
