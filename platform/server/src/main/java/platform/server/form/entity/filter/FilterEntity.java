package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.serialization.ServerCustomSerializable;

import java.util.HashSet;
import java.util.Set;

public abstract class FilterEntity implements Instantiable<FilterInstance>, ServerCustomSerializable {

    protected abstract void fillObjects(Set<ObjectEntity> objects);

    public abstract FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory);

    public Set<ObjectEntity> getObjects() {
        Set<ObjectEntity> objects = new HashSet<ObjectEntity>();
        fillObjects(objects);
        return objects;
    }
}
