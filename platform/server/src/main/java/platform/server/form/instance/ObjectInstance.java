package platform.server.form.instance;

import platform.interop.FormEventType;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.CalcProperty;
import platform.server.session.SessionChanges;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.*;

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

    public static <K> Map<ObjectInstance, Expr> getObjectValueExprs(Collection<ObjectInstance> objects) {
        Map<ObjectInstance, Expr> result = new HashMap<ObjectInstance, Expr>();
        for(ObjectInstance object : objects)
            result.put(object, object.getExpr());
        return result;
    }


    public boolean isNull() {
        return getObjectValue() instanceof NullValue;
    }

    public abstract ValueClass getGridClass();

    public abstract void changeValue(SessionChanges session, ObjectValue changeValue) throws SQLException;

    public abstract boolean classChanged(Collection<CalcProperty> changedProps);

    public abstract Type getType();

    protected boolean objectInGrid(Set<GroupObjectInstance> gridGroups) {
        return GroupObjectInstance.getUpTreeGroups(gridGroups).contains(groupTo);
    }

    public boolean objectUpdated(Set<GroupObjectInstance> gridGroups) { return !objectInGrid(gridGroups) && (updated & UPDATED_OBJECT)!=0; }
    public boolean dataUpdated(Collection<CalcProperty> changedProps) { return false; }
    public void fillProperties(Set<CalcProperty> properties) { }

    protected Expr getExpr() {
        return getObjectValue().getExpr();
    }

    public Expr getExpr(Map<ObjectInstance, ? extends Expr> classSource, Modifier modifier) {
        Expr result;
        if(classSource!=null && (result = classSource.get(this))!=null)
            return result;
        else
            return getExpr();
    }

    public GroupObjectInstance getApplyObject() {
        return groupTo;
    }

    public Collection<ObjectInstance> getObjectInstances() {
        return Collections.singletonList(this);
    }
}
