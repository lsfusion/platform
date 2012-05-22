package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ActionPropertyObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.instance.ActionPropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;

import java.util.Map;

import static platform.base.BaseUtils.crossJoin;

public class ActionPropertyMapImplement<P extends PropertyInterface, T extends PropertyInterface> extends ActionPropertyImplement<P, T> implements PropertyInterfaceImplement<T> {

    public ActionPropertyMapImplement(ActionProperty<P> property) {
        super(property);
    }

    public ActionPropertyMapImplement(ActionProperty<P> property, Map<P, T> mapping) {
        super(property, mapping);
    }

    public <K extends PropertyInterface> ActionPropertyMapImplement<P, K> map(Map<T, K> remap) {
        return new ActionPropertyMapImplement<P, K>(property, BaseUtils.join(mapping, remap));
    }

    public <L extends PropertyInterface> void mapEventAction(CalcPropertyMapImplement<L, T> where, int options) {
        property.setEventAction(where.map(BaseUtils.reverse(mapping)), options);
    }

    public ActionPropertyObjectEntity<P> mapObjects(Map<T, ? extends PropertyObjectInterfaceEntity> mapObjects) {
        return new ActionPropertyObjectEntity<P>(property, BaseUtils.join(mapping, mapObjects));
    }

    public CalcPropertyMapImplement<?, T> mapWhereProperty() {
        return property.getWhereProperty().map(mapping);
    }
}
