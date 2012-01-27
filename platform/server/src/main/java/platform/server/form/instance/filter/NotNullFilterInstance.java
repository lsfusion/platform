package platform.server.form.instance.filter;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

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

    @Override
    public <X extends PropertyInterface> Set<PropertyValueImplement<?>> getResolveChangeProperties(Property<X> toChange) {
        if(checkChange && Property.depends(property.property, toChange))
            return BaseUtils.immutableCast(Collections.singleton(property.getValueImplement()));
        return super.getResolveChangeProperties(toChange);
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
