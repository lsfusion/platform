package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// what the form the action runs in sends its client for a property, by its name in the form: the value of the cell the
// client is sent for the form's current objects (PropertyDrawInstance.getReaderProperty) - where the form builds a
// selection for the property, the list of its options -, written into sentInForm as text, and the function it tells the
// client to draw the property by - a render function given in CUSTOM, the platform's element of the property's
// selection (select<type><element>), or none -, written into renderInForm, of a selection of several values the text
// its value joins the options by, written into separatorInForm, and the parameters of a selection - the words multi,
// notNull, html, changeValue of the ones it has, none for none -, written into selectInForm
public class ReadSentAction extends InternalAction {

    public ReadSentAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String name = (String) getParam(0, context);
        FormInstance form = context.getFormInstance(false, true);
        PropertyDrawInstance<?> property = form.getPropertyDraw(name);
        Object value = property.getReaderProperty().read(form);
        try {
            findProperty("sentInForm[STRING]").change(value != null ? value.toString() : null, context, new DataObject(name));
            findProperty("renderInForm[STRING]").change(property.entity.getCustomRenderFunction(form.context), context, new DataObject(name));
            PropertyDrawEntity.Select select = property.entity.getSelectProperty(form.context);
            findProperty("separatorInForm[STRING]").change(select != null ? select.separator : null, context, new DataObject(name));
            findProperty("selectInForm[STRING]").change(select != null ? getParameters(select) : null, context, new DataObject(name));
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    // of several values, of one value that has to be there, its options viewed as HTML, a pick of one value given to the
    // property's explicit change as the option's view
    private static String getParameters(PropertyDrawEntity.Select select) {
        List<String> parameters = new ArrayList<>();
        if (select.type.equals("Multi"))
            parameters.add("multi");
        if (select.type.isEmpty())
            parameters.add("notNull");
        if (select.html)
            parameters.add("html");
        if (select.changeValue)
            parameters.add("changeValue");
        return parameters.isEmpty() ? "none" : String.join(" ", parameters);
    }
}
