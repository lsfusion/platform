package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.form.entity.ActionPropertyObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.linear.LAP;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.List;
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

    public <L extends PropertyInterface> void mapEventAction(CalcPropertyMapImplement<L, T> where, boolean session, int options) {
        property.setEventAction(where.map(BaseUtils.reverse(mapping)), new OrderedMap<CalcPropertyInterfaceImplement<P>, Boolean>(), false, session, options);
    }

    public ActionPropertyObjectEntity<P> mapObjects(Map<T, ? extends PropertyObjectInterfaceEntity> mapObjects) {
        return new ActionPropertyObjectEntity<P>(property, BaseUtils.join(mapping, mapObjects));
    }

    public CalcPropertyMapImplement<?, T> mapWhereProperty() {
        return property.getWhereProperty().map(mapping);
    }

    public LAP<P> createLP(List<T> listInterfaces) {
        return new LAP<P>(property, BaseUtils.mapList(listInterfaces, BaseUtils.reverse(mapping)));
    }
    
    public void execute(PropertyChange<T> change, ExecutionEnvironment env, FormEnvironment<T> form) throws SQLException {
        env.execute(property, change.map(mapping), form==null ? null : form.map(mapping));
    }
}
