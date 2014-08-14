package lsfusion.server.form.entity;

import lsfusion.base.BaseUtils;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.actions.ChangeReadObjectActionProperty;
import lsfusion.server.logics.property.actions.ExplicitActionProperty;

import java.util.Set;

public class ObjectEntity extends IdentityObject implements PropertyObjectInterfaceEntity {

    public GroupObjectEntity groupTo;

    public String caption;

    public String getCaption() {
        return !BaseUtils.isRedundantString(caption)
               ? caption
               : !BaseUtils.isRedundantString(baseClass.toString())
                 ? baseClass.toString()
                 : ServerResourceBundle.getString("logics.undefined.object");
    }

    public ValueClass baseClass;

    public ObjectEntity() {

    }
    
    public ObjectEntity(int ID, ValueClass baseClass, String caption) {
        this(ID, null, baseClass, caption);
    }

    public ObjectEntity(int ID, String sID, ValueClass baseClass, String caption) {
        super(ID);
        this.sID = sID != null ? sID : "obj" + ID;
        this.caption = caption;
        this.baseClass = baseClass;
    }

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.add(this);
    }

    public PropertyObjectInterfaceEntity getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return this == oldObject
                ? newObject
                : getInstance(instanceFactory).getDataObject();
    }

    @Override
    public String toString() {
        return getCaption();
    }

    @IdentityInstanceLazy
    public ExplicitActionProperty getChangeAction(Property filterProperty) {
        assert baseClass instanceof CustomClass;
        return new ChangeReadObjectActionProperty((CalcProperty) filterProperty, baseClass.getBaseClass());
    }

    @Override
    public AndClassSet getAndClassSet() {
        return baseClass.getUpSet();
    }

    public ResolveClassSet getResolveClassSet() {
        return baseClass.getResolveSet();
    }
}
