package lsfusion.server.form.entity;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.ServerLoggers;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.actions.DefaultChangeObjectActionProperty;
import lsfusion.server.logics.property.actions.ExplicitActionProperty;
import lsfusion.server.session.Modifier;

import java.util.Set;

import static lsfusion.server.logics.i18n.LocalizedString.create;

public class ObjectEntity extends IdentityObject implements PropertyObjectInterfaceEntity, ObjectSelector {

    public GroupObjectEntity groupTo;

    public LocalizedString caption;

    public LocalizedString getCaption() {
        return (caption != null && !BaseUtils.isRedundantString(caption.getSourceString()))
               ? caption
               : !BaseUtils.isRedundantString(baseClass.toString())
                 ? create(baseClass.toString())
                 : create("{logics.undefined.object}");
    }

    public ValueClass baseClass;

    public ObjectEntity() {

    }
    
    public ObjectEntity(int ID, ValueClass baseClass, LocalizedString caption) {
        this(ID, null, baseClass, caption);
    }

    public ObjectEntity(int ID, String sID, ValueClass baseClass, LocalizedString caption) {
        super(ID);
        this.sID = sID != null ? sID : "obj" + ID;
        this.caption = caption;
        this.baseClass = baseClass;
    }

    public ObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.add(this);
    }

    public PropertyObjectInterfaceInstance getRemappedInstance(ObjectEntity oldObject, ObjectInstance newObject, InstanceFactory instanceFactory) {
        return this == oldObject
                ? newObject
                : getInstance(instanceFactory).getDataObject();
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(getCaption());
    }

    @IdentityInstanceLazy
    public ExplicitActionProperty getChangeAction(Property filterProperty) {
        assert baseClass instanceof CustomClass;
        return new DefaultChangeObjectActionProperty((CalcProperty) filterProperty, baseClass.getBaseClass(), this);
    }

    @Override
    public AndClassSet getAndClassSet() {
        return baseClass.getUpSet();
    }

    public ResolveClassSet getResolveClassSet() {
        return baseClass.getResolveSet();
    }

    @Override
    public Type getType() {
        return baseClass.getType();
    }

    @Override
    public Expr getExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier, ImMap<ObjectEntity, ObjectValue> mapObjects) {
        Expr expr = mapExprs.get(this);
        if(expr != null)
            return expr;
        ServerLoggers.assertLog(false, "EXPR SHOULD EXIST IN ALL USE CASES");
        return mapObjects.get(this).getExpr();
    }

    @Override
    public ObjectValue getObjectValue(ImMap<ObjectEntity, ObjectValue> mapObjects) {
        ObjectValue objectValue = mapObjects.get(this);
        if(objectValue != null)
            return objectValue;
        return NullValue.instance;
    }

    @Override
    public GroupObjectEntity getApplyObject(ImOrderSet<GroupObjectEntity> groups) {
        return groupTo;
    }
}
