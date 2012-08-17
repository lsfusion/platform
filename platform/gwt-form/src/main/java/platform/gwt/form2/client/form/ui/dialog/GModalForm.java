package platform.gwt.form2.client.form.ui.dialog;

import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.shared.view.GForm;

public class GModalForm extends GModalWindow {
    public GModalForm(GForm form, final WindowHiddenHandler hiddenHandler) {
        super(form.caption, hiddenHandler);

        GFormController editorForm = new GFormController(form, true) {
            @Override
            public void hideForm() {
                GModalForm.this.hide();
            }
        };

        ResizeLayoutPanel mainPane = new ResizeLayoutPanel();
        mainPane.setWidth("800px");
        mainPane.setHeight("600px");
        mainPane.setWidget(editorForm);

        setWidget(mainPane);
    }

    public static GModalForm showForm(GForm form, WindowHiddenHandler hiddenHandler) {
        GModalForm modalForm = new GModalForm(form, hiddenHandler);
        modalForm.center();
        return modalForm;
    }
}
