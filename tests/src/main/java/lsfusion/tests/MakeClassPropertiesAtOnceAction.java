package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.lang.reflect.Field;
import java.sql.SQLException;

// two threads asking a class for its property of being of that class at once, as two modules parsed at once that ask
// for it first do. It is made on the class's own monitor, so the test holds it, as a first one making it would, and a
// second one has to wait. The classes have their property - a private field, reached by reflection - unmade for the
// time of the test and put back afterwards. What the test saw goes into cpFailure
public class MakeClassPropertiesAtOnceAction extends AtOnceAction {

    public MakeClassPropertiesAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            CustomClass item = (CustomClass) findClass("CpItem");
            String failure = make(context, item, CustomClass.class.getDeclaredField("property"), item::getProperty);
            if (failure == null) {
                DataClass data = NumericClass.get(37, 11);
                failure = make(context, data, DataClass.class.getDeclaredField("property"), data::getProperty);
            }

            findProperty("cpFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String make(ExecutionContext<ClassPropertyInterface> context, ValueClass cls, Field field, Runnable ask) throws IllegalAccessException {
        field.setAccessible(true);
        Object property = field.get(cls);
        field.set(cls, null);
        try {
            return runWhileHeld(context, cls, "a thread made the property of " + cls + " while another one held the class", ask);
        } finally {
            field.set(cls, property);
        }
    }
}
