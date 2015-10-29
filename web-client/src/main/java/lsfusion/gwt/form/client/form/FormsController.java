package lsfusion.gwt.form.client.form;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.client.navigator.GNavigatorAction;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.window.GModalityType;

public interface FormsController {

    void openForm(String canonicalName, String formSID, GModalityType modalityType, NativeEvent nativeEvent);

    void openForm(GForm form, GModalityType modalityType, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler);

    void select(Widget tabContent);

    void executeNavigatorAction(GNavigatorAction action);
}
