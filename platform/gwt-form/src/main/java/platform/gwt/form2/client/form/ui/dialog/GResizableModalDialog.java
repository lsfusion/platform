package platform.gwt.form2.client.form.ui.dialog;

import platform.gwt.form2.client.form.FormsController;
import platform.gwt.form2.shared.view.GForm;

public class GResizableModalDialog extends GResizableModalForm {

    public GResizableModalDialog(FormsController formsController, GForm form, final WindowHiddenHandler hiddenHandler) {
        super(formsController, form, hiddenHandler);
    }

    public static GResizableModalDialog showDialog(FormsController formsController, GForm form, WindowHiddenHandler hiddenHandler) {
        GResizableModalDialog dlg = new GResizableModalDialog(formsController, form, hiddenHandler);
        dlg.center();
        return dlg;
    }
}
