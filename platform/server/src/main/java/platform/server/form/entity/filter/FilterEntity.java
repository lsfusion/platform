package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.Mapper;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public abstract class FilterEntity {

    public abstract FilterInstance doMapping(Mapper mapper) throws SQLException;

    protected abstract void fillObjects(Set<ObjectEntity> objects);
    
    public Set<ObjectEntity> getObjects() {
        Set<ObjectEntity> objects = new HashSet<ObjectEntity>();
        fillObjects(objects);
        return objects;
    }
}
