package platform.gwt.form.client.form.ui;

import platform.gwt.view.GForm;

public class GModalDialog extends GModalForm {

    public GModalDialog(GForm form, final WindowHiddenHandler hiddenHandler) {
        super(form, hiddenHandler);
    }

    public static GModalDialog showDialog(GForm form, WindowHiddenHandler hiddenHandler) {
        GModalDialog dlg = new GModalDialog(form, hiddenHandler);
        dlg.show();
        return dlg;
    }
}
