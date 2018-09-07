package lsfusion.server.form.entity.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.CalcPropertyObjectInstance;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.Instantiable;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.form.instance.filter.NotNullFilterInstance;
import lsfusion.server.form.instance.filter.PropertyFilterInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class FilterEntity<P extends PropertyInterface> implements Instantiable<FilterInstance> {

    public boolean checkChange;    
    public CalcPropertyObjectEntity<P> property;
    public boolean resolveAdd;

    // нельзя удалять - используется при сериализации
    public FilterEntity() {
    }

    public FilterEntity(CalcPropertyObjectEntity<P> property) {
        this(property, false, false);
    }

    public FilterEntity(CalcPropertyObjectEntity<P> property, boolean resolveAdd) {
        this(property, false, resolveAdd);
    }

    public FilterEntity(CalcPropertyObjectEntity<P> property, boolean checkChange, boolean resolveAdd) {
        this.property = property;
        this.resolveAdd = resolveAdd;
        this.checkChange = checkChange;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return new NotNullFilterInstance<>(instanceFactory.getInstance(property), checkChange, resolveAdd);
    }

    public CalcPropertyObjectEntity<P> getCalcPropertyObjectEntity() {
        return property;
    }
    public ContextFilter getRemappedContextFilter(final ObjectEntity oldObject, final ObjectEntity newObject, final InstanceFactory instanceOldFactory) {
        return new ContextFilter() {
            public FilterInstance getFilter(InstanceFactory instanceNewFactory) {
                return new NotNullFilterInstance<P>(property.getRemappedInstance(oldObject, newObject.getInstance(instanceNewFactory), instanceOldFactory), checkChange, resolveAdd);
            }
        };
    }

    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, ImMap<ObjectEntity, ObjectValue> mapObjects, Modifier modifier) throws SQLException, SQLHandledException {
        return property.getExpr(mapKeys, modifier, mapObjects).getWhere();
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        property.fillObjects(objects);
    }

    public Set<ObjectEntity> getObjects() {
        Set<ObjectEntity> objects = new HashSet<>();
        fillObjects(objects);
        return objects;
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        return form.getApplyObject(getObjects());
    }

    public GroupObjectEntity getApplyObject(FormEntity formEntity) {
        return formEntity.getApplyObject(getObjects());
    }
}
