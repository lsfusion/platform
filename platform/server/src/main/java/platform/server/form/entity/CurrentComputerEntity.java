package platform.server.form.entity;

import platform.server.form.instance.Mapper;
import platform.server.form.instance.PropertyObjectInterfaceInstance;

import java.util.Set;

public class CurrentComputerEntity implements PropertyObjectInterfaceEntity {

    private CurrentComputerEntity() {
    }
    public static final CurrentComputerEntity instance = new CurrentComputerEntity();

    public PropertyObjectInterfaceInstance doMapping(Mapper mapper) {
        return mapper.computer;
    }

    public void fillObjects(Set<ObjectEntity> objects) {
    }
}
