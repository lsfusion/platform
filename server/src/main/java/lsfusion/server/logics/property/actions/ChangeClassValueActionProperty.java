package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.Iterator;

public class ChangeClassValueActionProperty extends SystemExplicitActionProperty {

    private ClassPropertyInterface objectInterface;
    private ClassPropertyInterface classInterface;

    public ChangeClassValueActionProperty(String name, String caption, BaseClass baseClass) {
        super(name, caption, baseClass, baseClass.objectClass);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        objectInterface = i.next();
        classInterface = i.next();
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getBaseClass().getChildProps().toMap(false);
    }

    private BaseClass getBaseClass() {
        return (BaseClass)objectInterface.interfaceClass;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.changeClass(context.getObjectInstance(objectInterface), context.getDataKeyValue(objectInterface),
                getBaseClass().findConcreteClassID((Integer) context.getKeyObject(classInterface)));
    }

}
