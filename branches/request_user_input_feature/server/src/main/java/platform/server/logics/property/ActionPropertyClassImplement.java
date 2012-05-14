package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;

import java.util.List;

public class ActionPropertyClassImplement extends PropertyClassImplement<ClassPropertyInterface, ActionProperty> {

    public ActionPropertyClassImplement(ActionProperty property, List<ValueClassWrapper> classes, List<ClassPropertyInterface> interfaces) {
        super(property, classes, interfaces);
    }

    public LAP createLP(List<ValueClassWrapper> listInterfaces) {
        return new LAP(property, BaseUtils.mapList(listInterfaces, BaseUtils.reverse(mapping)));
    }
}
