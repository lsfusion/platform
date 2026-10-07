package lsfusion.tests;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.ArrayList;
import java.util.List;

// which of the server's walks of a form's design reach a component, said as the words of those that do - `found`
// (the component a client names by its id: FormView.findById), `read` (its readers are read:
// FormEntity.getPropertyComponents), `sent` (its visibility is sent: FormEntity.getBaseComponents) - and whether a
// client that names it hidden hides it (`hidable`: ComponentView.isUserHidable), and a failure unless they are the
// ones expected. What the walks reach is the design's, never a session's, so only java can ask it
public class DesignWalksAction extends InternalAction {

    public DesignWalksAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String formName = (String) getParam(0, context);
        String componentSID = (String) getParam(1, context);
        String expected = (String) getParam(2, context);

        FormEntity form = context.getBL().findForm(formName);
        ComponentView component = form.view.getComponentBySID(componentSID);
        List<String> walked = new ArrayList<>();
        if (form.view.findById(component.getID()) == component)
            walked.add("found");
        if (form.getPropertyComponents().contains(component))
            walked.add("read");
        if (form.getBaseComponents().contains(component))
            walked.add("sent");
        if (component.isUserHidable())
            walked.add("hidable");
        String actual = String.join(" ", walked);
        if (!actual.equals(expected))
            throw new RuntimeException("the walks of " + formName + " reach " + componentSID + " as '" + actual
                    + "', not as '" + expected + "'");
    }
}
