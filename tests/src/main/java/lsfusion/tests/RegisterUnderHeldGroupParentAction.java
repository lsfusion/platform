package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NFImpl;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.struct.group.AbstractNode;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;

// a property registered into a simple group while the test holds the monitor an NF read of the group's parent takes - the
// NF's list of changes, the NF itself once it is final -, as the parse threads register every action and property they
// make into the private group, each registration reading the group's parents to drop their caches. The groups are made
// as the start makes them (LogicsModule.addAbstractGroup, reached by reflection), the parent's caches filled before the
// registration : afterwards, the parent has to list the property. The groups and the property are the test's own. What
// the test saw goes into sgFailure
public class RegisterUnderHeldGroupParentAction extends AtOnceAction {

    public RegisterUnderHeldGroupParentAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Method addAbstractGroup = LogicsModule.class.getDeclaredMethod("addAbstractGroup", String.class, LocalizedString.class, Group.class);
            addAbstractGroup.setAccessible(true);
            String suffix = String.valueOf(System.nanoTime()); // a group's name is taken for good
            Group parent = (Group) addAbstractGroup.invoke(LM, "sgHeldParent" + suffix, LocalizedString.create("sg held parent", false), null);
            Group simple = (Group) addAbstractGroup.invoke(LM, "sgHeld" + suffix, LocalizedString.create("sg held", false), parent);
            Version version = LM.getVersion();
            simple.changeChildrenToSimple(version);

            Field parentField = AbstractNode.class.getDeclaredField("parent");
            parentField.setAccessible(true);
            Object parentNF = parentField.get(simple);
            Field changesField = NFImpl.class.getDeclaredField("changes");
            changesField.setAccessible(true);
            Object changes = changesField.get(parentNF);
            Object monitor = changes instanceof NFList ? changes : parentNF;
            Property<?> property = new StoredDataProperty(LocalizedString.create("sg held", false), new ValueClass[0], StringClass.text);

            parent.getActionOrProperties();
            String failure = runWithoutWaiting(context, monitor,
                    "a property registered into the simple group waited for the monitor of the group's parent",
                    () -> simple.add(property, version));
            if (failure == null && !parent.getActionOrProperties().contains(property))
                failure = "the parent of the simple group a property was registered into does not list it";

            findProperty("sgFailure[]").change(failure, context);
        } catch (ReflectiveOperationException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
