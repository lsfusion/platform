package platform.server.logics.property.actions;

import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.Iterator;

/**
 * User: DAle
 * Date: 03.04.12
 */

public class ChangeClassActionProperty extends CustomActionProperty {

    private ClassPropertyInterface objectInterface;
    private ClassPropertyInterface classInterface;
    
    public ChangeClassActionProperty(String name, String caption, BaseClass baseClass) {
        super(name, caption, new ValueClass[] {baseClass, baseClass.objectClass});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        objectInterface = i.next();
        classInterface = i.next();
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        context.changeClass(context.getObjectInstance(objectInterface), context.getKeyValue(objectInterface),
                (Integer) context.getKeyValue(classInterface).object);
    }
}
