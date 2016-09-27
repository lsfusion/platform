package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

// сбрасывает объект в null
public class DropObjectActionProperty extends SystemExplicitActionProperty {

    public DropObjectActionProperty(ValueClass valueClass) {
        super(LocalizedString.create("{logics.property.actions.drop} " + valueClass), new ValueClass[]{valueClass});
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        PropertyObjectInterfaceInstance objectInstance = context.getSingleObjectInstance();
        if(objectInstance instanceof ObjectInstance) // не changeObject чтобы fire не вызывать, временно так
            context.getFormInstance().seekObject((ObjectInstance)objectInstance, NullValue.instance);
    }
}
