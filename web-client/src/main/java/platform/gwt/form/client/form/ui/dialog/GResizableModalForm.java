package platform.gwt.form.client.form.ui.dialog;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import platform.gwt.form.client.form.FormsController;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.view.GForm;

public class GResizableModalForm extends GResizableModalWindow {

    protected final ResizeLayoutPanel mainPane;

    protected GFormController editorForm;

    public GResizableModalForm(FormsController formsController, GForm form, final WindowHiddenHandler hiddenHandler) {
        super(form.caption, hiddenHandler);

        editorForm = new GFormController(formsController, form, true) {
            @Override
            public void hideForm() {
                super.hideForm();
                GResizableModalForm.this.hide();
            }

            @Override
            protected void onInitialFormChangesReceived() {
                int formWidth = formLayout.getMainContainerWidth();
                int formHeight = formLayout.getMainContainerHeight();
                if (formWidth < mainPane.getOffsetWidth() && formHeight < mainPane.getOffsetHeight()) {
                    setContentSize(formWidth, formHeight);
                    center();
                }
                super.onInitialFormChangesReceived();
                initialFormChangesReceived();
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

    public static GResizableModalForm showForm(FormsController formsController, GForm form, final WindowHiddenHandler hiddenHandler) {
        GResizableModalForm modalForm = new GResizableModalForm(formsController, form, hiddenHandler);
        modalForm.center();
        return modalForm;
    }

    public void initialFormChangesReceived() {
    }
}
