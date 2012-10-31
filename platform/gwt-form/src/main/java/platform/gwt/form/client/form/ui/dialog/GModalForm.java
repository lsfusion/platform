package platform.gwt.form.client.form.ui.dialog;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import platform.gwt.form.client.form.FormsController;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.view.GForm;

public class GModalForm extends GModalWindow {

    protected final ResizeLayoutPanel mainPane;

    public GModalForm(FormsController formsController, GForm form, final WindowHiddenHandler hiddenHandler) {
        super(form.caption, hiddenHandler);

        GFormController editorForm = new GFormController(formsController, form, true) {
            @Override
            public void hideForm() {
                GModalForm.this.hide();
            }
        };

        int width = Math.min(Window.getClientWidth() - 20, editorForm.getPreferredWidth() == -1 ? 800 : editorForm.getPreferredWidth());
        int height = Math.min(Window.getClientHeight() - 100, editorForm.getPreferredHeight() == -1 ? 600 : editorForm.getPreferredHeight());

        mainPane = new ResizeLayoutPanel();
        mainPane.setPixelSize(width, height);
        mainPane.setWidget(editorForm);

        setWidget(mainPane);
    }

    public static GModalForm showForm(FormsController formsController, GForm form, WindowHiddenHandler hiddenHandler) {
        GModalForm modalForm = new GModalForm(formsController, form, hiddenHandler);
        modalForm.center();
        return modalForm;
    }
}
