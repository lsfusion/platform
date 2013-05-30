package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;

public class OrFilterEntity extends OpFilterEntity<OrFilterEntity> {

    public OrFilterEntity() {
    }

    public OrFilterEntity(FilterEntity op1, FilterEntity op2) {
        super(op1, op2);
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    @Override
    public FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return new OrFilterEntity(op1.getRemappedFilter(oldObject, newObject, instanceFactory), op2.getRemappedFilter(oldObject, newObject, instanceFactory));
    }
}
