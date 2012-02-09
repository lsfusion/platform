package platform.server.logics.property.actions.flow;

import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.PropertyUtils;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.join;
import static platform.base.BaseUtils.nullJoin;
import static platform.base.BaseUtils.reverse;

public abstract class FlowActionProperty extends ActionProperty {

    protected <I extends PropertyInterface> FlowActionProperty(String sID, String caption, List<I> listInterfaces, Collection<PropertyInterfaceImplement<I>> used) {
        super(sID, caption, getClasses(listInterfaces, used));
    }
}
