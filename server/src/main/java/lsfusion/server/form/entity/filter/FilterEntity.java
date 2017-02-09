package lsfusion.server.form.entity.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.Instantiable;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public abstract class FilterEntity implements Instantiable<FilterInstance> {

    protected abstract void fillObjects(Set<ObjectEntity> objects);

    public abstract FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory);

    public Set<ObjectEntity> getObjects() {
        Set<ObjectEntity> objects = new HashSet<>();
        fillObjects(objects);
        return objects;
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        return form.getApplyObject(getObjects());
    }

    public abstract Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, ImMap<ObjectEntity, ObjectValue> mapObjects, Modifier modifier) throws SQLException, SQLHandledException;

    public GroupObjectEntity getApplyObject(FormEntity formEntity) {
        return formEntity.getApplyObject(getObjects());
    }
}
