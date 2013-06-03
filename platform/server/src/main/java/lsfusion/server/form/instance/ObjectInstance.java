package lsfusion.server.form.instance;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.SessionChanges;

import java.sql.SQLException;

// на самом деле нужен collection но при extend'е нужна конкретная реализация
public abstract class ObjectInstance extends CellInstance<ObjectEntity> implements PropertyObjectInterfaceInstance {

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид
    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_CLASS = (1 << 1);
    public final static int UPDATED_GRIDCLASS = (1 << 3);

    protected int updated = UPDATED_CLASS | UPDATED_GRIDCLASS;

    public GroupObjectInstance groupTo;

    public String getCaption() {
        return entity.getCaption();
    }

    public ObjectInstance(ObjectEntity entity) {
        super(entity);
        this.entity = entity;
    }

    public String toString() {
        return getCaption();
    }

    public abstract ValueClass getBaseClass();

    public abstract ObjectValue getObjectValue();
    public DataObject getDataObject() {
        return (DataObject)getObjectValue();
    }

    public static <K> ImMap<ObjectInstance, Expr> getObjectValueExprs(ImSet<ObjectInstance> objects) {
        return objects.mapValues(new GetValue<Expr, ObjectInstance>() {
            public Expr getMapValue(ObjectInstance value) {
                return value.getExpr();
            }});
    }


    public boolean isNull() {
        return getObjectValue() instanceof NullValue;
    }

    public abstract ValueClass getGridClass();

    public abstract void changeValue(SessionChanges session, ObjectValue changeValue) throws SQLException;

    public abstract boolean classChanged(FunctionSet<CalcProperty> changedProps);

    public abstract Type getType();

    protected boolean objectInGrid(ImSet<GroupObjectInstance> gridGroups) {
        return GroupObjectInstance.getUpTreeGroups(gridGroups).contains(groupTo);
    }

    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) { return !objectInGrid(gridGroups) && (updated & UPDATED_OBJECT)!=0; }
    public boolean dataUpdated(FunctionSet<CalcProperty> changedProps) { return false; }
    public void fillProperties(MSet<CalcProperty> properties) { }

    protected Expr getExpr() {
        return getObjectValue().getExpr();
    }

    public Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier) {
        Expr result;
        if(classSource!=null && (result = classSource.get(this))!=null)
            return result;
        else
            return getExpr();
    }

    public GroupObjectInstance getApplyObject() {
        return groupTo;
    }

    public ImSet<ObjectInstance> getObjectInstances() {
        return SetFact.singleton(this);
    }
}
