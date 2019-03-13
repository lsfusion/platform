package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.form.interactive.instance.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.Instantiable;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.NotNullFilterInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.CalcPropertyObjectEntity;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.action.session.change.modifier.Modifier;

import java.sql.SQLException;

public class FilterEntity<P extends PropertyInterface> implements Instantiable<FilterInstance> {

    public boolean checkChange;    
    private CalcPropertyObjectEntity<P> property;
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

    public CalcPropertyObjectEntity<P> getImportProperty() {
        return property;
    }
    public ContextFilter getRemappedContextFilter(final ObjectEntity oldObject, final ObjectEntity newObject, final InstanceFactory instanceOldFactory) {
        return new ContextFilter() {
            public FilterInstance getFilter(InstanceFactory instanceNewFactory) {
                return new NotNullFilterInstance<P>(property.getRemappedInstance(oldObject, newObject.getInstance(instanceNewFactory), instanceOldFactory), checkChange, resolveAdd);
            }
        };
    }

    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier) throws SQLException, SQLHandledException {
        return property.getEntityExpr(mapKeys, modifier).getWhere();
    }

    public ImSet<ObjectEntity> getObjects() {
        return property.getObjectInstances();
    }

    public GroupObjectEntity getApplyObject(FormEntity formEntity) {
        return getApplyObject(formEntity, SetFact.<GroupObjectEntity>EMPTY());
    }

    public GroupObjectEntity getApplyObject(FormEntity formEntity, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return formEntity.getApplyObject(getObjects(), excludeGroupObjects);
    }
}
