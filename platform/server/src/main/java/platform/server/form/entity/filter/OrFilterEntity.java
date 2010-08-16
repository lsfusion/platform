package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.OrFilterInstance;
import platform.server.form.instance.Mapper;

import java.sql.SQLException;
import java.util.Set;

public class OrFilterEntity extends FilterEntity {

    FilterEntity op1;
    FilterEntity op2;

    public OrFilterEntity(FilterEntity op1, FilterEntity op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    public FilterInstance doMapping(Mapper mapper) throws SQLException {
        return new OrFilterInstance(op1.doMapping(mapper),op2.doMapping(mapper));
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        op1.fillObjects(objects);
        op2.fillObjects(objects);
    }
}
