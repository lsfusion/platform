package lsfusion.server.logics.form.struct.object;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.ExplicitAction;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.interactive.action.change.DefaultChangeObjectAction;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectInterfaceEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import static lsfusion.server.physics.dev.i18n.LocalizedString.create;

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

    public String integrationSID;

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
                : getInstance(instanceFactory).getObjectValue();
    }

    @Override
    public String toString() {
        return ThreadLocalContext.localize(getCaption());
    }

    @IdentityInstanceLazy
    public ExplicitAction getChangeAction() {
        return new DefaultChangeObjectAction(this);
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

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getIntegrationSID() {
        return integrationSID != null ? integrationSID : getSID();
    }

    @Override
    public boolean noClasses() {
        return noClasses;
    }

    public boolean isSimpleList() {
        return groupTo.getObjects().size() == 1 && groupTo.viewType.isList() && !groupTo.isInTree();
    }
}
