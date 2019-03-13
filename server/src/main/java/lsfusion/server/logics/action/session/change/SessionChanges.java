package lsfusion.server.logics.action.session.change;

import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ConcreteObjectClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.interactive.change.ChangedData;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
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
    ChangedData update(FormInstance toUpdate) throws SQLException;
}
