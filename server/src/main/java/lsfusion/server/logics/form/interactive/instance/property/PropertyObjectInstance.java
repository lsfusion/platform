package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.changed.ChangedData;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.exec.hint.AutoHintsAspect;

import java.sql.SQLException;

public class PropertyObjectInstance<P extends PropertyInterface> extends ActionOrPropertyObjectInstance<P, Property<P>> implements OrderInstance {

    public PropertyObjectInstance(Property<P> property, ImMap<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    public PropertyObjectInstance<P> getRemappedPropertyObject(ImMap<? extends PropertyObjectInterfaceInstance, ? extends ObjectValue> mapKeyValues) {
        return new PropertyObjectInstance<>(property, remapSkippingEqualsObjectInstances(mapKeyValues));
    }

    public Object read(FormInstance formInstance) throws SQLException, SQLHandledException {
        return property.read(formInstance, getInterfaceObjectValues());
    }

    public ValueClass getValueClass() {
        return property.getValueClass(ClassType.formPolicy);
    }

    private Expr getExpr(final ImMap<ObjectInstance, ? extends Expr> classSource, final Modifier modifier, WhereBuilder whereBuilder) throws SQLException, SQLHandledException {

        ImMap<P, Expr> joinImplement = mapping.mapValuesEx((ThrowingFunction<PropertyObjectInterfaceInstance, Expr, SQLException, SQLHandledException>) value -> value.getExpr(classSource, modifier));
        return property.getExpr(joinImplement, modifier, whereBuilder);
    }

    public Expr getExpr(final ImMap<ObjectInstance, ? extends Expr> classSource, final Modifier modifier) throws SQLException, SQLHandledException {
        return getExpr(classSource, modifier, (WhereBuilder)null);
    }
    public Expr getExpr(final ImMap<ObjectInstance, ? extends Expr> classSource, final Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        return getExpr(classSource, modifier, reallyChanged, null);
    }
    public Expr getExpr(final ImMap<ObjectInstance, ? extends Expr> classSource, final Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        WhereBuilder changedWhere = null;
        if(reallyChanged!=null && !reallyChanged.containsChange(this))
            changedWhere = new WhereBuilder();
        if(mUsedProps != null)
            mUsedProps.add(property);
        Expr expr = getExpr(classSource, modifier, changedWhere);
        if(changedWhere!=null)
            if(!changedWhere.toWhere().isFalse())
                reallyChanged.addChange(this);
        return expr;
    }

    public boolean isReallyChanged(boolean hidden, Modifier modifier, ReallyChanged reallyChanged, final ImSet<GroupObjectInstance> groupObjects) throws SQLException, SQLHandledException {
        if(reallyChanged.containsChange(this))
            return true;

        assert !objectUpdated(groupObjects); // так как по всему flow в том или ином виде сначала проверяются groupObjects

        boolean disableHint = hidden && Settings.get().isDisableHiddenHintReallyChanged();
            
        ImRevMap<ObjectInstance,KeyExpr> keys = KeyExpr.getMapKeys(getObjectInstances().toSet().filterFn(element -> element.objectInGrid(groupObjects)));
        WhereBuilder changedWhere = new WhereBuilder();
        if(disableHint) { // hack - needed because finding out if property really changed with hints can be a huge overhead (for example if it is hidden), so we'll disable hints
            modifier.getPropertyChanges(); // hack - however reading propertyChanges can lead to notifySourceChange where hints can be used, so will read it before disabling
            AutoHintsAspect.pushDisabledRepeat();
        }
        try {
            Expr result = getExpr(keys, modifier, changedWhere);
            if(result == null)
                return true;
        } finally {
            if(disableHint)
                AutoHintsAspect.popDisabledRepeat();
        }
        return !changedWhere.toWhere().isFalse();
    } 

    // проверяет на то что изменился верхний объект
    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        for(PropertyObjectInterfaceInstance intObject : mapping.valueIt())
            if(intObject.objectUpdated(gridGroups)) return true;

        return false;
    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        for(PropertyObjectInterfaceInstance intObject : mapping.valueIt())
            if(intObject.classUpdated(gridGroups))
                return true;

        return false;
    }

    public void fillProperties(MSet<Property> properties) {
        properties.add(property);
    }

    public boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier, boolean hidden, ImSet<GroupObjectInstance> groupObjects) throws SQLException, SQLHandledException {
        if(changedProps.externalProps.contains(property))
            return true;

        if(!changedProps.props.contains(property))
            return false;
        
        if(changedProps.wasRestart)
            return true;
        
        return isReallyChanged(hidden, modifier, reallyChanged, groupObjects); // cache пока не используем так как за многим надо следить
    }

    public Type getType() {
        return property.getType();
    }
}
