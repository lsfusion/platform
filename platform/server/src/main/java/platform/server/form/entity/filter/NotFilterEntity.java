package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.NotFilterInstance;
import platform.server.form.instance.Mapper;

import java.sql.SQLException;
import java.util.Set;

public class NotFilterEntity extends FilterEntity {

    FilterEntity filter;

    public NotFilterEntity(FilterEntity filter) {
        this.filter = filter;
    }

    public FilterInstance doMapping(Mapper mapper) throws SQLException {
        return new NotFilterInstance(filter.doMapping(mapper));
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        filter.fillObjects(objects);
    }
}
