package platform.server.session;

import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.data.type.Type;
import platform.server.classes.CustomClass;
import platform.server.data.SQLSession;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.navigator.ObjectNavigator;
import platform.base.BaseUtils;

import java.sql.SQLException;
import java.util.Map;
import java.util.Collection;

public abstract class PropertyChange<T extends PropertyInterface,P extends Property<T>> implements DataChange {
    public final P property;
    protected final Map<T,DataObject> mapping;

    public PropertyChange(P property, Map<T, DataObject> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public void change(DataSession session, TableModifier<? extends TableChanges> modifier, Object newValue, boolean externalID) throws SQLException {
        change(session, session.getObjectValue(newValue, property.getType()), externalID, modifier);
    }

    public abstract void change(DataSession session, ObjectValue newValue, boolean externalID, TableModifier<? extends TableChanges> modifier) throws SQLException;

    public abstract Collection<FilterNavigator> getFilters(ObjectNavigator valueObject,BusinessLogics<?> BL);

    public abstract CustomClass getDialogClass();

    public Integer read(SQLSession session, TableModifier<? extends TableChanges> modifier) throws SQLException {
        return (Integer) property.read(session, mapping, modifier);
    }

    public Type getType() {
        return property.getType();
    }

    @Override
    public String toString() {
        return property.toString() +" <" + BaseUtils.toString(mapping.values(),",") + ">";
    }
}
