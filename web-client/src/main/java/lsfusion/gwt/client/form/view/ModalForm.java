package lsfusion.gwt.client.form.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableModalWindow;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.view.MainFrame;

public class ModalForm extends FormContainer<ResizableModalWindow> {


    public ModalForm(FormsController formsController, Long requestIndex, String caption, GModalityType modalityType, boolean async) {
        super(formsController, requestIndex, modalityType, async);

        contentWidget.setCaption(caption);
    }

    @Override
    protected ResizableModalWindow initContentWidget() {
        ResizableModalWindow window = new ResizableModalWindow() {
            @Override
            protected void onShow() {
                initMaxPreferredSize(); // we need after attach to have correct sizes

                super.onShow();
            }
        };
        if(async) {
            GwtClientUtils.setThemeImage("loading.gif", imageUrl -> window.setContentWidget(createLoadingWidget(imageUrl)), false);
            window.setDefaultSize();
        }
        return window;
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
