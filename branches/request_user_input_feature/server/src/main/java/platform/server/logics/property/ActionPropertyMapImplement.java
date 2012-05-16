package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.instance.ActionPropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.crossJoin;

public class ActionPropertyMapImplement<T extends PropertyInterface> extends ActionPropertyImplement<T> implements PropertyInterfaceImplement<T> {

    public ActionPropertyMapImplement(ActionProperty property) {
        super(property);
    }

    public ActionPropertyMapImplement(ActionProperty property, Map<ClassPropertyInterface, T> mapping) {
        super(property, mapping);
    }

    public <K extends PropertyInterface> ActionPropertyMapImplement<K> map(Map<T, K> remap) {
        return new ActionPropertyMapImplement<K>(property, BaseUtils.join(mapping, remap));
    }

    public <L extends PropertyInterface> void mapEventAction(CalcPropertyMapImplement<L, T> where, int options) {
        property.setEventAction(where.map(BaseUtils.reverse(mapping)), options);
    }

    public ActionPropertyObjectInstance mapObjects(Map<T, ? extends PropertyObjectInterfaceInstance> mapObjects) {
        return new ActionPropertyObjectInstance(property, BaseUtils.join(mapping, mapObjects));
    }

    // дублирующие Calc
    public Map<T,ValueClass> mapCommonInterfaces() {
        return crossJoin(mapping, property.getMapClasses());
    }
}
