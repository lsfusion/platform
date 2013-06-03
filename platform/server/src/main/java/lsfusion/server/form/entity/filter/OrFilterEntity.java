package lsfusion.server.form.entity.filter;

import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.filter.FilterInstance;

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
