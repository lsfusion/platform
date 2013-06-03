package lsfusion.gwt.form.client.form;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.client.navigator.GNavigatorAction;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.window.GModalityType;

public interface FormsController {

    void openForm(String formSID, GModalityType modalityType);

    void openForm(GForm form, GModalityType modalityType, WindowHiddenHandler hiddenHandler);

    void select(Widget tabContent);

    void executeNavigatorAction(GNavigatorAction action);
}
