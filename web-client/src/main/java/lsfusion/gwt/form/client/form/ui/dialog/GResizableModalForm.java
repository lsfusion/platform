package lsfusion.gwt.form.client.form.ui.dialog;

import com.google.gwt.user.client.Window;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.form.client.form.FormsController;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.GForm;

import static java.lang.Math.min;

public class GResizableModalForm extends GResizableModalWindow {

    protected GFormController form;

    public GResizableModalForm(FormsController formsController, GForm gForm, final WindowHiddenHandler hiddenHandler) {
        super(gForm.caption, hiddenHandler);

        form = new GFormController(formsController, gForm, true) {
            @Override
            public void hideForm() {
                super.hideForm();
                GResizableModalForm.this.hide();
            }

            @Override
            protected void onInitialFormChangesReceived() {
                super.onInitialFormChangesReceived();
                initialFormChangesReceived();
            }
        };

        setContentWidget(form);

        //сразу добавляем в DOM, чтобы можно было посчитать естественную ширину элементов
        attach();
    }

    @Override
    protected void onLoad() {
        if (initialOnLoad) {
            Dimension size = form.getPreferredSize();
            if (size.width > 0) {
                int wndWidth = Window.getClientWidth();
                size.width = min(size.width, wndWidth - 20);
                form.setWidth(size.width + "px");
            }
            if (size.height > 0) {
                int wndHeight = Window.getClientHeight();
                size.height = min(size.height, wndHeight - 100);
                form.setHeight(size.height + "px");
            }
        }
        super.onLoad();
    }

    public void initialFormChangesReceived() {
    }

    public static GResizableModalForm showForm(FormsController formsController, GForm form, final WindowHiddenHandler hiddenHandler) {
        GResizableModalForm modalForm = new GResizableModalForm(formsController, form, hiddenHandler);
        modalForm.justCenter();
        return modalForm;
    }
}
