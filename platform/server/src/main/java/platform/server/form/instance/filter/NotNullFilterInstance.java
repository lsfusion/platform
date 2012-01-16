package platform.server.form.instance.filter;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.where.Where;
import platform.server.form.entity.ClassFormEntity;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.logics.property.derived.OnChangeProperty;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class NotNullFilterInstance<P extends PropertyInterface> extends PropertyFilterInstance<P> {

    private final boolean checkChange;

    public NotNullFilterInstance(PropertyObjectInstance<P> property) {
        this(property, false);
    }

    public NotNullFilterInstance(PropertyObjectInstance<P> property, boolean checkChange) {
        super(property);
        this.checkChange = checkChange;
    }

    public NotNullFilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
        super(inStream, form);
        checkChange = false;
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return property.getExpr(mapKeys, modifier).getWhere();
    }

    public <X extends PropertyInterface> Set<? extends FilterEntity> getResolveChangeFilters(ClassFormEntity<?> formEntity, PropertyValueImplement<X> implement) {
        if(checkChange && Property.depends(property.property, implement.property)) {
            PropertyValueImplement<P> filterImplement = property.getValueImplement();
            OnChangeProperty<P, X> onChangeProperty = filterImplement.property.getOnChangeProperty(implement.property);
            return Collections.singleton(
                            new NotNullFilterEntity<OnChangeProperty.Interface<P, X>>(
                                    onChangeProperty.getPropertyObjectEntity(filterImplement.mapping, implement.mapping, formEntity.object)
                            )
            );
        }
        return super.getResolveChangeFilters(formEntity, implement);
    }

    @Override
    public void resolveAdd(DataSession session, Modifier modifier, CustomObjectInstance object, DataObject addObject) throws SQLException {

        if (!hasObjectInInterface(object))
            return;

        Map<P, KeyExpr> mapKeys = property.property.getMapKeys();
        Map<PropertyObjectInterfaceInstance, KeyExpr> mapObjects = BaseUtils.crossJoin(property.mapping, mapKeys);
        property.property.setNotNull(mapKeys, getChangedWhere(object, mapObjects, addObject), session);
    }
}
