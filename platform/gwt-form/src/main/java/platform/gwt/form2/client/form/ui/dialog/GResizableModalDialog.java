package platform.gwt.form2.client.form.ui.dialog;

import platform.gwt.form2.shared.view.GForm;

public class GResizableModalDialog extends GResizableModalForm {

    public GResizableModalDialog(GForm form, final WindowHiddenHandler hiddenHandler) {
        super(form, hiddenHandler);
    }

    public static GResizableModalDialog showDialog(GForm form, WindowHiddenHandler hiddenHandler) {
        GResizableModalDialog dlg = new GResizableModalDialog(form, hiddenHandler);
        dlg.center();
        return dlg;
    }
}
