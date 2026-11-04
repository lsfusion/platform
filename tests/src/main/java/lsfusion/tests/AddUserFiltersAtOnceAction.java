package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.struct.AutoFinalFormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// two threads giving one property of a form a user filter at once, as two modules parsed at once that give one property
// a user filter do. A form the server has started with is finished and takes no more filters, so the test builds a form
// of its own, as a parse does. The form's ids are swapped for ones that hold the first thread as it makes the filter's
// component - past the check for one made already - until the second thread either is inside too, past that check - a
// second filter of the property - or waits for the form's design the filters are made under. What the test saw goes
// into ufFailure, and so does a run that did not have the two at once
public class AddUserFiltersAtOnceAction extends AtOnceAction {

    public AddUserFiltersAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            BaseLogicsModule baseLM = context.getBL().LM;
            Version version = baseLM.getVersion();
            AutoFinalFormEntity form = new AutoFinalFormEntity(LocalizedString.create("Filters"), baseLM);
            ObjectEntity object = form.addSingleGroupObject(findClass("UfItem"));
            PropertyDrawEntity<?, ?> property = form.addPropertyDraw(findProperty("ufName[UfItem]"),
                                                                     SetFact.singletonOrder(object));

            Hold hold = new Hold();
            IDGenerator ids = form.genID;
            form.genID = () -> hold.call(ids::id);
            String failure = runAtOnce(context, hold, form.view, "two threads made filters of one property at once",
                    () -> form.addUserFilter(property, version),
                    () -> form.addUserFilter(property, version));
            if (failure == null) {
                int filters = 0;
                for (FilterView filter : form.view.get(object.groupTo).grid.filters.getNFOrderSet(version))
                    if (filter.property == form.view.get(property))
                        filters++;
                if (filters != 1)
                    failure = "the property got " + filters + " filters";
            }

            findProperty("ufFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
