package lsfusion.server.logics.property.classes.user;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.classes.SingleClassExpr;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.where.classes.IsClassWhere;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.table.TableOwner;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.event.LinkType;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.AbstractDataProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.exec.db.table.DBTable;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.sql.SQLException;

// virtual "data property" storing object class
// strictly system property, on application level ObjectClassProperty should be used
public class ClassDataProperty extends AbstractDataProperty implements ObjectClassField {

    public final ObjectValueClassSet set;

    public ClassDataProperty(LocalizedString caption, ObjectValueClassSet set) {
        super(caption, SetFact.singletonOrder(new ClassPropertyInterface(0, set.getOr().getCommonClass())));
        this.set = set;
    }

    public boolean isStored() {
        return true;
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        throw new RuntimeException("should not be");
    }

    @Override
    protected ClassWhere<Object> getDataClassValueWhere() {
        return new ClassWhere<>(MapFact.<Object, AndClassSet>toMap(interfaces.single(), set, "value", set.getBaseClass().objectClass));
    }

    public Expr getInconsistentExpr(Expr expr) {
        return getInconsistentExpr(MapFact.singleton(interfaces.single(), expr), set.getBaseClass());
    }
    
    public void dropInconsistentClasses(SQLSession session, BaseClass baseClass, KeyExpr key, Where where, OperationOwner owner) throws SQLException, SQLHandledException {
        DBTable table = baseClass.getInconsistentTable(mapTable.table);
        session.modifyRecords(new ModifyQuery(table, new Query<>(MapFact.singletonRev(table.keys.single(), key), MapFact.singleton(field, Expr.NULL()), where), owner, TableOwner.global));
    }

    public Expr getStoredExpr(Expr expr) {
        return getStoredExpr(MapFact.singleton(interfaces.single(), expr));
    }

    @Override
    protected ImCol<Pair<ActionOrProperty<?>, LinkType>> calculateLinks(boolean events) {
        if(events)
            return getActionChangeProps();
        return SetFact.EMPTY();
    }

    public PropertyField getField() {
        return field;
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

    public ClassDataProperty getProperty() {
        return this;
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("should not be");
    }

    public Where getIsClassWhere(SingleClassExpr expr, ObjectValueClassSet set, IsClassType type) {
        return new IsClassWhere(expr, set, type);
    }

    @Override
    public String getChangeExtSID() {
        assert false;
        return null; // по идее всегда canonical name есть
    }
}
