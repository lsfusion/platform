package lsfusion.server.logics.form.interactive.instance.object;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.change.SessionChanges;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.changed.ChangedData;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Function;

// на самом деле нужен collection но при extend'е нужна конкретная реализация
public abstract class ObjectInstance extends CellInstance<ObjectEntity> implements PropertyObjectInterfaceInstance {

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид
    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_CLASS = (1 << 1);
    public final static int UPDATED_GRIDCLASS = (1 << 3);

    public int updated = UPDATED_CLASS | UPDATED_GRIDCLASS;

    public GroupObjectInstance groupTo;

    public LocalizedString getCaption() {
        return entity.getCaption();
    }

    public boolean noClasses = false;

    public ObjectInstance(ObjectEntity entity) {
        super(entity);
        this.entity = entity;
        this.noClasses = entity.noClasses();
    }

    public String toString() {
        return ThreadLocalContext.localize(getCaption());
    }

    public abstract ValueClass getBaseClass();

    public abstract ObjectValue getObjectValue();
    public DataObject getDataObject() {
        ObjectValue objectValue = getObjectValue();
        if(!(objectValue instanceof DataObject))
            ServerLoggers.assertLog(false, "OBJECT  " + toString() + " IS NULL");
        return (DataObject) objectValue;
    }

    public static <K> ImMap<ObjectInstance, Expr> getObjectValueExprs(ImSet<ObjectInstance> objects) {
        return objects.mapValues((Function<ObjectInstance, Expr>) ObjectInstance::getExpr);
    }

    public abstract ValueClass getGridClass();

    public abstract void changeValue(SessionChanges session, FormInstance form, ObjectValue changeValue) throws SQLException, SQLHandledException;

    public abstract boolean classChanged(ChangedData changedProps);

    public abstract Type getType();

    public boolean objectInGrid(ImSet<GroupObjectInstance> gridGroups) {
        return GroupObjectInstance.getUpTreeGroups(gridGroups).contains(groupTo);
    }

    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) { return !objectInGrid(gridGroups) && (updated & UPDATED_OBJECT)!=0; }
    public boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier, boolean hidden, ImSet<GroupObjectInstance> groupObjects) { return false; }
    public void fillProperties(MSet<Property> properties) { }

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

    @Override
    public Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier, ReallyChanged reallyChanged) {
        return getExpr(classSource, modifier);
    }

    @Override
    public Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) {
        return getExpr(classSource, modifier);
    }

    public abstract ConcreteClass getCurrentClass();

    public GroupObjectInstance getApplyObject() {
        return groupTo;
    }

    public ImSet<ObjectInstance> getObjectInstances() {
        return SetFact.singleton(this);
    }
}
