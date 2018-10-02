package lsfusion.server.form.entity;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.identity.IdentityObject;
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
import lsfusion.server.logics.property.actions.DefaultChangeObjectActionProperty;
import lsfusion.server.logics.property.actions.ExplicitActionProperty;
import lsfusion.server.session.Modifier;

import static lsfusion.server.logics.i18n.LocalizedString.create;

public class ObjectEntity extends IdentityObject implements PropertyObjectInterfaceEntity, ObjectSelector {

    public GroupObjectEntity groupTo;

    public LocalizedString caption;

    public LocalizedString getCaption() {
        return (caption != null && !BaseUtils.isRedundantString(caption.getSourceString()))
               ? caption
               : baseClass != null && !BaseUtils.isRedundantString(baseClass.toString())
                 ? create(baseClass.toString())
                 : create("{logics.undefined.object}");
    }

    public ValueClass baseClass;
    private boolean noClasses;

    public ObjectEntity() {

    }
    
    public ObjectEntity(int ID, ValueClass baseClass, LocalizedString caption) {
        this(ID, null, baseClass, caption);
    }
    public ObjectEntity(int ID, ValueClass baseClass, LocalizedString caption, boolean noClasses) {
        this(ID, null, baseClass, caption, noClasses);
    }
    public ObjectEntity(int ID, String sID, ValueClass baseClass, LocalizedString caption) {
        this(ID, sID, baseClass, caption, false);
    }
    public ObjectEntity(int ID, String sID, ValueClass baseClass, LocalizedString caption, boolean noClasses) {
        super(ID);
        this.sID = sID != null ? sID : "obj" + ID;
        this.caption = caption;
        this.baseClass = baseClass;
        this.noClasses = noClasses;
    }

    public ObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
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
    public ExplicitActionProperty getChangeAction() {
        assert baseClass instanceof CustomClass;
        return new DefaultChangeObjectActionProperty(baseClass.getBaseClass(), this);
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
        if(noClasses())
            return null;
        return baseClass.getType();
    }
    
    @Override
    public Expr getEntityExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier) {
        return mapExprs.get(this);
    }

    @Override
    public ObjectValue getObjectValue(ImMap<ObjectEntity, ? extends ObjectValue> mapObjects) {
        ObjectValue objectValue = mapObjects.get(this);
        if(objectValue != null)
            return objectValue;
        return NullValue.instance;
    }

    @Override
    public GroupObjectEntity getApplyObject(FormEntity formEntity, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return groupTo;
    }

    @Override
    public boolean noClasses() {
        return noClasses;
    }
}
