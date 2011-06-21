package platform.server.form.instance.filter;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.entity.AbstractClassFormEntity;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.logics.property.derived.OnChangeProperty;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
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

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) {
        return property.getExpr(mapKeys, modifier).getWhere();
    }

    public <X extends PropertyInterface> Set<? extends FilterEntity> getResolveChangeFilters(AbstractClassFormEntity<?> formEntity, PropertyValueImplement<X> implement) {
        if(checkChange && Property.depends(property.property, implement.property)) {
            PropertyValueImplement<P> filterImplement = property.getValueImplement();
            OnChangeProperty<P, X> onChangeProperty = filterImplement.property.getOnChangeProperty(implement.property);
            return Collections.singleton(
                            new NotNullFilterEntity<OnChangeProperty.Interface<P, X>>(
                                    onChangeProperty.getPropertyObjectEntity(filterImplement.mapping, implement.mapping, formEntity.getObject())
                            )
            );
        }
        return super.getResolveChangeFilters(formEntity, implement);
    }
}
