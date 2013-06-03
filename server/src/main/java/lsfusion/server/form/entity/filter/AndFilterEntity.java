package lsfusion.server.form.entity.filter;

import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.filter.FilterInstance;

public class AndFilterEntity extends OpFilterEntity<AndFilterEntity> {

    public AndFilterEntity() {
    }

    public AndFilterEntity(FilterEntity op1, FilterEntity op2) {
        super(op1, op2);
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    @Override
    public FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return new AndFilterEntity(op1.getRemappedFilter(oldObject, newObject, instanceFactory), op2.getRemappedFilter(oldObject, newObject, instanceFactory));
    }

}
