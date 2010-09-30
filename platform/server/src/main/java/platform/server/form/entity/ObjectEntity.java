package platform.server.form.entity;

import platform.base.IdentityObject;
import platform.server.classes.ValueClass;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInterfaceInstance;

import java.util.Set;

public class ObjectEntity extends IdentityObject implements PropertyObjectInterfaceEntity {

    public GroupObjectEntity groupTo;

    public String caption;

    public boolean addOnTransaction = false;
    public boolean resetOnApply = false;

    public final ValueClass baseClass;

    public ObjectEntity(int ID, ValueClass baseClass, String caption) {
        super(ID);
        this.caption = caption;
        this.baseClass = baseClass;
    }

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.add(this);
    }    
}
