package platform.server.form.entity;

import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInterfaceInstance;

import java.util.Set;

public class CurrentComputerEntity implements PropertyObjectInterfaceEntity {

    private CurrentComputerEntity() {
    }
    public static final CurrentComputerEntity instance = new CurrentComputerEntity();

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void fillObjects(Set<ObjectEntity> objects) {
    }
}
