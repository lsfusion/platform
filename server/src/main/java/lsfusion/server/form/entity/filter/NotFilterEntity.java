package lsfusion.server.form.entity.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.serialization.ServerSerializationPool;
import lsfusion.server.session.Modifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

public class NotFilterEntity extends FilterEntity {

    public FilterEntity filter;

    public NotFilterEntity() {
        
    }
    
    public NotFilterEntity(FilterEntity filter) {
        this.filter = filter;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        filter.fillObjects(objects);
    }

    @Override
    public FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return new NotFilterEntity(filter.getRemappedFilter(oldObject, newObject, instanceFactory));
    }

    @Override
    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, ImMap<ObjectEntity, ObjectValue> mapObjects, Modifier modifier) throws SQLException, SQLHandledException {
        return filter.getWhere(mapKeys, mapObjects, modifier).not();
    }
}
