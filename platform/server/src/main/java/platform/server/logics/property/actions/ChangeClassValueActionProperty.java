package platform.server.logics.property.actions;

import platform.base.QuickSet;
import platform.server.classes.*;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.Iterator;

public class ChangeClassValueActionProperty extends SystemActionProperty {

    private ClassPropertyInterface objectInterface;
    private ClassPropertyInterface classInterface;

    public ChangeClassValueActionProperty(String name, String caption, BaseClass baseClass) {
        super(name, caption, new ValueClass[] {baseClass, baseClass.objectClass});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        objectInterface = i.next();
        classInterface = i.next();
    }

    @Override
    public QuickSet<CalcProperty> aspectChangeExtProps() {
        return getBaseClass().getChildProps();
    }

    private BaseClass getBaseClass() {
        return (BaseClass)objectInterface.interfaceClass;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.changeClass(context.getObjectInstance(objectInterface), context.getKeyValue(objectInterface),
                getBaseClass().findConcreteClassID((Integer) context.getKeyValue(classInterface).object));
    }

}
