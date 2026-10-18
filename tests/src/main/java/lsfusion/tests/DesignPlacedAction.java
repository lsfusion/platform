package lsfusion.tests;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

// the container of a form's design a component is in, both by the names DESIGN gives them (`PROPERTY(name(i))`,
// `POPUP(i)`), and a failure unless it is the one expected. The design is the form's, never a session's, so only java
// can ask it
public class DesignPlacedAction extends InternalAction {

    public DesignPlacedAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String formName = (String) getParam(0, context);
        String componentSID = (String) getParam(1, context);
        String expected = (String) getParam(2, context);

        FormEntity form = context.getBL().findForm(formName);
        ContainerView container = form.view.getComponentBySID(componentSID).getContainer();
        String actual = container != null ? container.getSID() : null;
        if (!expected.equals(actual))
            throw new RuntimeException(formName + " places " + componentSID + " in " + actual + ", not in " + expected);
    }
}
