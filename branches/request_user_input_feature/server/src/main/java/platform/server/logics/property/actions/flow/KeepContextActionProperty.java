package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyInterfaceImplement;

import java.util.Collection;
import java.util.List;

public abstract class KeepContextActionProperty extends FlowActionProperty {

    protected KeepContextActionProperty(String sID, String caption, int size) {
        super(sID, caption, size);
    }
}
