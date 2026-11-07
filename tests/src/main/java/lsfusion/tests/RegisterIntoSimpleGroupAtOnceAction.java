package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NFSimpleOrderSetImpl;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.lang.reflect.Field;
import java.sql.SQLException;

// a property registered into a simple group while its children's list is held by the test, as the parse threads
// register every action and property they make into the private group. The list is read by reflection and its monitor
// held : a synchronized list locks itself, and registration must finish while that monitor is held, or the parse
// threads would queue. The group has to list the property afterwards. The group and the property are the test's own.
// What the test saw goes into sgFailure
public class RegisterIntoSimpleGroupAtOnceAction extends AtOnceAction {

    public RegisterIntoSimpleGroupAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Version version = context.getBL().LM.getVersion();
            Group simple = new Group("SimpleGroups.sgAtOnce", LocalizedString.create("sg at once"));
            simple.changeChildrenToSimple(version);
            Field childrenField = Group.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            Field listField = NFSimpleOrderSetImpl.class.getDeclaredField("list");
            listField.setAccessible(true);
            Object list = listField.get(childrenField.get(simple));
            Property<?> property = new StoredDataProperty(LocalizedString.create("sg", false), new ValueClass[0], StringClass.text);

            String failure = runWithoutWaiting(context, list,
                    "a property registered into the simple group waited for the monitor of its children",
                    () -> simple.add(property, version));
            if (failure == null && !simple.getActionOrProperties().contains(property))
                failure = "the simple group a property was registered into does not list it";

            findProperty("sgFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
