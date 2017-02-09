package lsfusion.server.form.entity.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;

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

    @Override
    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, ImMap<ObjectEntity, ObjectValue> mapObjects, Modifier modifier) throws SQLException, SQLHandledException {
        return op1.getWhere(mapKeys, mapObjects, modifier).or(op2.getWhere(mapKeys, mapObjects, modifier));
    }
}
