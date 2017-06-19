package lsfusion.server.form.entity.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.IsClassProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;

public class IsClassFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public CustomClass isClass;

    public IsClassFilterEntity(CalcPropertyObjectEntity<P> property, CustomClass isClass, boolean resolveAdd) {
        super(property, resolveAdd);
        this.isClass = isClass;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    @Override
    public FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return new IsClassFilterEntity<>(property.getRemappedEntity(oldObject, newObject, instanceFactory), isClass, resolveAdd);
    }

    @Override
    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, ImMap<ObjectEntity, ObjectValue> mapObjects, Modifier modifier) throws SQLException, SQLHandledException {
        return IsClassProperty.getWhere(isClass, property.getExpr(mapKeys, modifier, mapObjects), modifier, null);
    }
}
