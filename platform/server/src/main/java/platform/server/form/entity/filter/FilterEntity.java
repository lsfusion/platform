package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.filter.FilterInstance;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public abstract class FilterEntity implements Instantiable<FilterInstance> {

    protected abstract void fillObjects(Set<ObjectEntity> objects);
    
    public Set<ObjectEntity> getObjects() {
        Set<ObjectEntity> objects = new HashSet<ObjectEntity>();
        fillObjects(objects);
        return objects;
    }
}
