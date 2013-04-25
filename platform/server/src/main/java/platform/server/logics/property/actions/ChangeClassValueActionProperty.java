package platform.server.logics.property.actions;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.BaseClass;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.Iterator;

public class ChangeClassValueActionProperty extends SystemActionProperty {

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
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.changeClass(context.getObjectInstance(objectInterface), context.getKeyValue(objectInterface),
                getBaseClass().findConcreteClassID((Integer) context.getKeyValue(classInterface).object));
    }

}
