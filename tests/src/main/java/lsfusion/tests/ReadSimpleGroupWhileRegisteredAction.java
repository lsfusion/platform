package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
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
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// a simple group read while a property is registered into it, as the start reads the whole set while dialog forms
// register their actions, and as a drill-down action registers itself on its first execution. The read is held inside
// the group's last child - a group of the test's own, after a property - while another property goes into the same
// children : the read has to go on, and as it began before the registration, a read of the group and of its parent
// afterwards has to list the property all the same, after the first one. The groups and the properties are the test's
// own. What the test saw goes into sgFailure
public class ReadSimpleGroupWhileRegisteredAction extends AtOnceAction {

    public ReadSimpleGroupWhileRegisteredAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Version version = context.getBL().LM.getVersion();
            Group parent = new Group("SimpleGroups.sgParent", LocalizedString.create("sg parent"));
            Group simple = new Group("SimpleGroups.sgSimple", LocalizedString.create("sg simple"));
            simple.changeChildrenToSimple(version);
            parent.add(simple, version);
            Property<?> before = new StoredDataProperty(LocalizedString.create("sg before", false), new ValueClass[0], StringClass.text);
            simple.add(before, version);
            Hold hold = new Hold();
            Group holding = new Group("SimpleGroups.sgHolding", LocalizedString.create("sg holding")) {
                @Override
                public ImOrderSet<ActionOrProperty> getActionOrProperties() {
                    return hold.call(super::getActionOrProperties);
                }
            };
            simple.add(holding, version);
            Property<?> during = new StoredDataProperty(LocalizedString.create("sg during", false), new ValueClass[0], StringClass.text);

            String failure = runWhileHolding(context, hold, parent::getActionOrProperties, () -> simple.add(during, version));
            if (failure == null && !simple.getActionOrProperties().equals(ListFact.toList(before, during)))
                failure = "the group read while a property was registered into it lists " + simple.getActionOrProperties();
            if (failure == null && !parent.getActionOrProperties().equals(ListFact.toList(before, during)))
                failure = "the parent of the group read while a property was registered into it lists " + parent.getActionOrProperties();

            findProperty("sgFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
