package lsfusion.server.form.entity.filter;

import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.Instantiable;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.serialization.ServerCustomSerializable;

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

    public GroupObjectEntity getToDraw(FormEntity form) {
        return form.getApplyObject(getObjects());
    }
}
