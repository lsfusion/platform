package platform.gwt.form2.client.form;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form2.shared.view.GForm;
import platform.gwt.form2.shared.view.window.GModalityType;

public interface FormsController {

    void openForm(String formSID, GModalityType modalityType);

    void openForm(GForm form, GModalityType modalityType, WindowHiddenHandler hiddenHandler);

    void select(Widget tabContent);
}
