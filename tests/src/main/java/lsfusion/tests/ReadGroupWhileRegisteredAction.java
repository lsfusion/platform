package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// a group read while a property is registered in a group under it, as a start task reads the whole set while dialog
// forms register their actions. The test builds a group of two simple ones, as the private group is : one the property
// is registered in, and one whose read holds the reader - past the first one - until the registration is done. The read
// is of the group's properties, a view of its whole set; a read of either afterwards has to list the property. The groups
// and the property are the test's own. What the test saw goes into gmFailure
public class ReadGroupWhileRegisteredAction extends AtOnceAction {

    public ReadGroupWhileRegisteredAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Version version = context.getBL().LM.getVersion();
            Group group = new Group("GroupMemos.gmGroup", LocalizedString.create("gm"));
            Group registered = new Group("GroupMemos.gmRegistered", LocalizedString.create("gm registered"));
            registered.changeChildrenToSimple(version);
            Hold hold = new Hold();
            Group holding = new Group("GroupMemos.gmHolding", LocalizedString.create("gm holding")) {
                @Override
                public ImOrderSet<ActionOrProperty> getActionOrProperties() {
                    return hold.call(super::getActionOrProperties);
                }
            };
            holding.changeChildrenToSimple(version);
            group.add(registered, version);
            group.add(holding, version);
            Property<?> property = PropertyFact.createStatic(LocalizedString.create("gm", false), StringClass.text).property;

            String failure = runWhileHolding(context, hold, group::getProperties, () -> registered.add(property, version));
            if (failure == null && !group.getActionOrProperties().contains(property))
                failure = "the group read while the property was registered under it kept what it read";
            if (failure == null && !group.getProperties().contains(property))
                failure = "the group read while the property was registered under it kept the properties it read";

            findProperty("gmFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
