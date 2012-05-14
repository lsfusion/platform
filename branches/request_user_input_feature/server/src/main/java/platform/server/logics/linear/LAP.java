package platform.server.logics.linear;

import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;

import java.util.List;

public class LAP extends LP<ClassPropertyInterface, ActionProperty> {

    public LAP(ActionProperty property) {
        super(property);
    }

    public LAP(ActionProperty property, List<ClassPropertyInterface> listInterfaces) {
        super(property, listInterfaces);
    }
}
