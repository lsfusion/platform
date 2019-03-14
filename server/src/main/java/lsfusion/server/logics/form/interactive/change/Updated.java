package lsfusion.server.logics.form.interactive.change;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.property.Property;

import java.sql.SQLException;

public interface Updated {

    // изменилось что-то влияющее на isInInterface/getClassSet (класс верхних объектов или класс grid'а)
    boolean classUpdated(ImSet<GroupObjectInstance> gridGroups);
    // изменилось что-то использующее в getExpr конкретные value (один из верхних объектов)
    boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups);
    boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier, boolean hidden, ImSet<GroupObjectInstance> groupObjects) throws SQLException, SQLHandledException;
    
    void fillProperties(MSet<Property> properties);

    boolean isInInterface(GroupObjectInstance classGroup);
}
