package lsfusion.server.logics.property;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.SingleClassExpr;
import lsfusion.server.data.expr.where.extra.IsClassWhere;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.session.PropertyChanges;

import java.sql.SQLException;

// первично свойство, соответствующее полю хранящему значение класса
// строго системное свойство, в логике предполагается использование ObjectClassProperty
public class ClassDataProperty extends CalcProperty<ClassPropertyInterface> implements ObjectClassField {

    public final ObjectValueClassSet set;

    public ClassDataProperty(String caption, ObjectValueClassSet set) {
        super(caption, SetFact.singletonOrder(new ClassPropertyInterface(0, set.getOr().getCommonClass())));
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

    public ClassWhere<Object> calcClassValueWhere(CalcClassType calcType) {
        return new ClassWhere<Object>(MapFact.<Object, AndClassSet>toMap(interfaces.single(), set, "value", set.getBaseClass().objectClass));
    }

    public Table.Join.Expr getInconsistentExpr(Expr expr) {
        return getInconsistentExpr(MapFact.singleton(interfaces.single(), expr), set.getBaseClass());
    }
    
    public void dropInconsistentClasses(SQLSession session, BaseClass baseClass, KeyExpr key, Where where, OperationOwner owner) throws SQLException, SQLHandledException {
        Table table = baseClass.getInconsistentTable(mapTable.table);
        session.modifyRecords(new ModifyQuery(table, new Query<KeyField, PropertyField>(MapFact.singletonRev(table.keys.single(), key), MapFact.singleton(field, Expr.NULL), where), owner, TableOwner.global));
    }

    public Table.Join.Expr getStoredExpr(Expr expr) {
        return (Table.Join.Expr) getStoredExpr(MapFact.singleton(interfaces.single(), expr));
    }

    @Override
    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks(boolean calcEvents) {
        return getActionChangeProps();
    }

    public BaseExpr getFollowExpr(BaseExpr joinExpr) {
        return (BaseExpr) joinExpr.classExpr(this);
    }

    public ObjectValueClassSet getObjectSet() {
        return set;
    }

    public ImplementTable getTable() {
        return mapTable.table;
    }

    public CalcProperty getProperty() {
        return this;
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("should not be");
    }

    public Where getIsClassWhere(SingleClassExpr expr, ObjectValueClassSet set, boolean inconsistent) {
        return new IsClassWhere(expr, set, inconsistent);
    }

    @Override
    public String getChangeExtSID() {
        assert false;
        return null; // по идее всегда canonical name есть
    }
}
