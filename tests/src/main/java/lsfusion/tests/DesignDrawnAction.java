package lsfusion.tests;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

// who draws a property of a form, by its name in the form, under the React views of its design: `react` - a React view
// draws it from its data -, `platform` - the platform draws it and a React view places it -, or `none` - no React view
// is there; and a failure unless it is the one expected. The design is the form's, so only java can ask it
public class DesignDrawnAction extends InternalAction {

    public DesignDrawnAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String formName = (String) getParam(0, context);
        String propertySID = (String) getParam(1, context);
        String expected = (String) getParam(2, context);

        FormEntity form = context.getBL().findForm(formName);
        PropertyDrawEntity<?, ?> property = form.getPropertyDraw(propertySID);
        FormView<?> view = form.view;
        String actual = view.isReactDrawn(property) ? "react" : view.isPlatformDrawn(view.get(property)) ? "platform" : "none";
        if (!expected.equals(actual))
            throw new RuntimeException(formName + " has " + propertySID + " drawn by '" + actual + "', not by '" + expected + "'");
    }
}
