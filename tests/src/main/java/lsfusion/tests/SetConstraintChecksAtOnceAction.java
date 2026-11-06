package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

// two threads putting constraints on one property at once, as two modules parsed at once that put constraints on one
// property do. What a constraint checks the property against is written on the property's own monitor, so the test
// holds it, as a first constraint would, and a second one has to wait. The property's checks are put back afterwards.
// What the test saw goes into ccFailure
public class SetConstraintChecksAtOnceAction extends AtOnceAction {

    public SetConstraintChecksAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Property<?> negative = findProperty("ccNegative[]").property;
            ImSet<Property<?>> checked = SetFact.singleton(findProperty("ccName[]").property);
            Property.CheckType type = negative.checkChange;
            ImSet<Property<?>> properties = negative.checkProperties;
            String failure;
            try {
                failure = runWhileHeld(context, negative, "a thread put a constraint's checks on the property while another one held it",
                        () -> negative.setCheckChange(Property.CheckType.CHECK_SOME, checked));
                if (failure == null && (negative.checkChange != Property.CheckType.CHECK_SOME || negative.checkProperties != checked))
                    failure = "the property kept the checks " + negative.checkChange + " " + negative.checkProperties;
            } finally {
                negative.setCheckChange(type, properties);
            }

            findProperty("ccFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
