package platform.gwt.form2.client.form.ui.dialog;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.shared.view.GForm;

public class GResizableModalForm extends GResizableModalWindow {

    protected final ResizeLayoutPanel mainPane;

    public GResizableModalForm(GForm form, final WindowHiddenHandler hiddenHandler) {
        super(form.caption, hiddenHandler);

        GFormController editorForm = new GFormController(form, true) {
            @Override
            public void hideForm() {
                GResizableModalForm.this.hide();
            }
        };

        int wndWidth = Window.getClientWidth();
        int wndHeight = Window.getClientHeight();

        int width = Math.min(wndWidth - 20, editorForm.getPreferredWidth() == -1 ? wndWidth*7/10 : editorForm.getPreferredWidth());
        int height = Math.min(wndHeight - 100, editorForm.getPreferredHeight() == -1 ? wndHeight*7/10 : editorForm.getPreferredHeight());

        mainPane = new ResizeLayoutPanel();
        mainPane.setPixelSize(width, height);
        mainPane.setWidget(editorForm);

        setContentWidget(mainPane);
    }

    public static GResizableModalForm showForm(GForm form, final WindowHiddenHandler hiddenHandler) {
        GResizableModalForm modalForm = new GResizableModalForm(form, hiddenHandler);
        modalForm.center();
        return modalForm;
    }
}
