package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.OrFilterInstance;
import platform.server.form.instance.InstanceFactory;

import java.sql.SQLException;
import java.util.Set;

public class OrFilterEntity extends FilterEntity {

    public FilterEntity op1;
    public FilterEntity op2;

    public OrFilterEntity(FilterEntity op1, FilterEntity op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        op1.fillObjects(objects);
        op2.fillObjects(objects);
    }
}
