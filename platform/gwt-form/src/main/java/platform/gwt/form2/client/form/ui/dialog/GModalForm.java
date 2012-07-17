package platform.gwt.form2.client.form.ui.dialog;

import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.client.form.ui.WindowHiddenHandler;
import platform.gwt.view2.GForm;

public class GModalForm extends GModalWindow {
    public GModalForm(GForm form, final WindowHiddenHandler hiddenHandler) {
        super(form.caption, hiddenHandler);

        addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                throw new IllegalStateException("should never be called!");
            }
        });

        GFormController editorForm = new GFormController(form, true) {
            @Override
            public void hideForm() {
                GModalForm.this.hide();
                GModalForm.this.destroy();
            }
        };

        addItem(editorForm);
    }

    public static GModalForm showForm(GForm form, WindowHiddenHandler hiddenHandler) {
        GModalForm modalForm = new GModalForm(form, hiddenHandler);
        modalForm.show();
        return modalForm;
    }
}
