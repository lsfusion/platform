package lsfusion.server.session;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.ChangedData;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.SessionDataProperty;

import java.sql.SQLException;

public interface SessionChanges {

    public void restart(boolean cancel, ImSet<SessionDataProperty> keep) throws SQLException;

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException, SQLHandledException;

    public void changeProperty(DataProperty property, PropertyChange<ClassPropertyInterface> keys) throws SQLException, SQLHandledException;

    public ConcreteClass getCurrentClass(DataObject value) throws SQLException, SQLHandledException;

    public DataObject getDataObject(ValueClass valueClass, Object value) throws SQLException, SQLHandledException;

    // узнает список изменений произошедших без него
    public ChangedData update(FormInstance<?> toUpdate) throws SQLException;

    public String applyMessage(BusinessLogics<?> BL) throws SQLException, SQLHandledException;
}
