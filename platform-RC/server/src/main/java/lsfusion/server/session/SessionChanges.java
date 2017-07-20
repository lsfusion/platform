package lsfusion.server.session;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.ChangedData;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.SessionDataProperty;

import java.sql.SQLException;

public interface SessionChanges {

    void restart(boolean cancel, FunctionSet<SessionDataProperty> keep) throws SQLException, SQLHandledException;

    void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException, SQLHandledException;

    void changeProperty(DataProperty property, PropertyChange<ClassPropertyInterface> keys) throws SQLException, SQLHandledException;

    ConcreteClass getCurrentClass(DataObject value) throws SQLException, SQLHandledException;

    DataObject getDataObject(ValueClass valueClass, Object value) throws SQLException, SQLHandledException;

    // узнает список изменений произошедших без него
    ChangedData update(FormInstance<?> toUpdate) throws SQLException;
}
