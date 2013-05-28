package platform.gwt.form.client.form;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form.client.navigator.GNavigatorAction;
import platform.gwt.form.shared.view.GForm;
import platform.gwt.form.shared.view.window.GModalityType;

public interface FormsController {

    void openForm(String formSID, GModalityType modalityType);

    void openForm(GForm form, GModalityType modalityType, WindowHiddenHandler hiddenHandler);

    void select(Widget tabContent);

    void executeNavigatorAction(GNavigatorAction action);
}
