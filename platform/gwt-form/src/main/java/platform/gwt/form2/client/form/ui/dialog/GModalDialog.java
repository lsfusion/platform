package platform.gwt.form2.client.form.ui.dialog;

import platform.gwt.form2.client.form.ui.WindowHiddenHandler;
import platform.gwt.view2.GForm;

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
