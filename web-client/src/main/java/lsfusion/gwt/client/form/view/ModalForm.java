package lsfusion.gwt.client.form.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.ResizableModalWindow;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.view.MainFrame;

import static java.lang.Math.min;

public class ModalForm extends FormContainer<ResizableModalWindow> {

    public ModalForm(FormsController formsController) {
        super(formsController);
    }

    @Override
    protected ResizableModalWindow initContentWidget() {
        return new ResizableModalWindow() {
            @Override
            protected void onShow() {
                initMaxPreferredSize(); // we need after attach to have correct sizes

                super.onShow();
            }
        };
    }

    @Override
    protected void setContent(Widget widget) {
        contentWidget.setContentWidget(widget);
    }

    private FormContainer prevForm;

    @Override
    public void show() {
        prevForm = MainFrame.getAssertCurrentForm();
        if(prevForm != null) // if there were no currentForm
            prevForm.onBlur(false);

        contentWidget.show();

        onFocus(true);
    }

    @Override
    public void hide() {
        onBlur(true);

        contentWidget.hide();

        if(prevForm != null)
            prevForm.onFocus(false);
    }

    public void setCaption(String caption, String tooltip) {
        contentWidget.setCaption(caption);
        contentWidget.setTooltip(tooltip);
    }
}
