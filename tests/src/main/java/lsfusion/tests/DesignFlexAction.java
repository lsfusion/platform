package lsfusion.tests;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

// the extension coefficient (flex) a component of a form's design gets, the component by the name DESIGN gives it
// (`BOX(i)`), and a failure unless it is the one expected. The design is the form's, never a session's, so only java
// can ask it
public class DesignFlexAction extends InternalAction {

    public DesignFlexAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String formName = (String) getParam(0, context);
        String componentSID = (String) getParam(1, context);
        int expected = (Integer) getParam(2, context);

        FormEntity form = context.getBL().findForm(formName);
        double actual = form.view.getComponentBySID(componentSID).getFlex(form.getGlobalContext());
        if (actual != expected)
            throw new RuntimeException(formName + " gives " + componentSID + " flex " + actual + ", not " + expected);
    }
}
