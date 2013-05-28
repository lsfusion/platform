package platform.server.session;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.FormInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.SessionDataProperty;

import java.sql.SQLException;

public interface SessionChanges {

    public void restart(boolean cancel, ImSet<SessionDataProperty> keep) throws SQLException;

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException;

    public void changeProperty(DataProperty property, PropertyChange<ClassPropertyInterface> keys) throws SQLException;

    public ConcreteClass getCurrentClass(DataObject value) throws SQLException;

    public DataObject getDataObject(ValueClass valueClass, Object value) throws SQLException;

    // узнает список изменений произошедших без него
    public ImSet<CalcProperty> update(FormInstance<?> toUpdate) throws SQLException;

    public String applyMessage(BusinessLogics<?> BL) throws SQLException;
}
