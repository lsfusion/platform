package platform.gwt.form2.client.form.ui.dialog;

import platform.gwt.form2.client.form.FormsController;
import platform.gwt.form2.shared.view.GForm;

public class GModalDialog extends GModalForm {

    public GModalDialog(FormsController formsController, GForm form, final WindowHiddenHandler hiddenHandler) {
        super(formsController, form, hiddenHandler);
    }

    public static GModalDialog showDialog(FormsController formsController, GForm form, WindowHiddenHandler hiddenHandler) {
        GModalDialog dlg = new GModalDialog(formsController, form, hiddenHandler);
        dlg.center();
        return dlg;
    }
}
