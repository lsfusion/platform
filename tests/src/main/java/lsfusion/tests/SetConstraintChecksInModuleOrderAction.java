package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// two modules that do not require each other putting constraints on one property, the later one in the module order
// writing first, as the parse threads' timing can have it : the later one's CHECKED, the earlier one's CHECKED BY ccName
// - the property keeps CHECKED. Not to change a property the server started with, the one constrained is made here. What
// the test saw goes into ccOrderFailure
public class SetConstraintChecksInModuleOrderAction extends AtOnceAction {

    public SetConstraintChecksInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LogicsModule[] modules = independentModules(context.getBL(), LM);
            ImSet<Property<?>> checked = SetFact.singleton(findProperty("ccName[]").property);

            Property<?> property = new StoredDataProperty(LocalizedString.NONAME, new ValueClass[0], StringClass.text);
            property.setCheckChange(Property.CheckType.CHECK_ALL, null, modules[1].getVersion());
            property.setCheckChange(Property.CheckType.CHECK_SOME, checked, modules[0].getVersion());

            String failure = property.getCheckChange() == Property.CheckType.CHECK_ALL && property.getCheckProperties() == null ? null :
                    "the property kept the checks " + property.getCheckChange() + " " + property.getCheckProperties() + " of the module earlier in the module order ("
                    + modules[0].getName() + "), written last, not CHECKED of " + modules[1].getName();
            findProperty("ccOrderFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
