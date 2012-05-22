package platform.server.logics.property.actions;

import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.classes.ObjectClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public abstract class CustomReadClassActionProperty extends CustomActionProperty {

    protected CustomReadClassActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    protected static class Read {
        private final CustomClass baseClass;
        private final CustomClass defaultClass;
        private final boolean concrete;

        public Read(CustomClass baseClass, CustomClass defaultClass, boolean concrete) {
            this.baseClass = baseClass;
            this.defaultClass = defaultClass;
            this.concrete = concrete;
        }

        public Read(CustomClass baseClass, boolean concrete) {
            this(baseClass, baseClass, concrete);
        }
    }
    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        ObjectClass readClass = null;
        Read read = getReadClass(context);
        if (read != null) {
            if(read.baseClass.hasChildren()) {
                ObjectValue objectValue = context.requestUserClass(read.baseClass, read.defaultClass, read.concrete);
                if (!(objectValue instanceof DataObject)) { // cancel
                    return;
                }

                readClass = read.baseClass.getBaseClass().findClassID((Integer) ((DataObject) objectValue).object);
            } else
                readClass = read.baseClass;
        }

        executeRead(context, readClass);
    }

    protected abstract Read getReadClass(ExecutionContext context);

    protected abstract void executeRead(ExecutionContext<ClassPropertyInterface> context, ObjectClass readClass) throws SQLException;

}
