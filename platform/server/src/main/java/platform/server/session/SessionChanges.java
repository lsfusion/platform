package platform.server.session;

import platform.base.QuickSet;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.CustomClass;
import platform.server.data.type.Type;
import platform.server.form.instance.FormInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.CalcProperty;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface SessionChanges {

    public void restart(boolean cancel, Set<SessionDataProperty> keep) throws SQLException;

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException;

    public void changeProperty(DataProperty property, PropertyChange<ClassPropertyInterface> keys) throws SQLException;

    public ConcreteClass getCurrentClass(DataObject value) throws SQLException;

    public DataObject getDataObject(Object value, Type type) throws SQLException;
    public ObjectValue getObjectValue(Object value, Type type) throws SQLException;

    // узнает список изменений произошедших без него
    public QuickSet<CalcProperty> update(FormInstance<?> toUpdate) throws SQLException;

    public String applyMessage(BusinessLogics<?> BL) throws SQLException;
}
