package platform.server.logics.property.actions;

import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

/**
 * User: DAle
 * Date: 03.04.12
 */

public class ChangeClassActionProperty extends CustomActionProperty {
    private ConcreteCustomClass cls;

    public ChangeClassActionProperty(String name, String caption, ConcreteCustomClass cls, BaseClass baseClass) {
        super(name, caption, new ValueClass[] {baseClass});
        this.cls = cls;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        context.getSession().changeClass(context.getSingleKeyValue(), cls);
    }
}
