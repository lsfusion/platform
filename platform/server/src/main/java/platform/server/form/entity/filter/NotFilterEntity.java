package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.NotFilterInstance;

import java.sql.SQLException;
import java.util.Set;

public class NotFilterEntity extends FilterEntity {

    public FilterEntity filter;

    public NotFilterEntity(FilterEntity filter) {
        this.filter = filter;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        filter.fillObjects(objects);
    }
}
