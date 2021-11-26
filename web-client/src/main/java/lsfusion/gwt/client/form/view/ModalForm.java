package lsfusion.gwt.client.form.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableModalWindow;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.view.MainFrame;

public class ModalForm extends FormContainer<ResizableModalWindow> {


    public ModalForm(FormsController formsController, GAsyncFormController asyncFormController, String caption, boolean async) {
        super(formsController, asyncFormController, async);

        if(async) {
            GwtClientUtils.setThemeImage(loadingAsyncImage, imageUrl -> setContent(createLoadingWidget(imageUrl)), false);
            contentWidget.setDefaultSize();
        }
        contentWidget.setCaption(caption);
    }

    @Override
    protected ResizableModalWindow initContentWidget() {
        ResizableModalWindow window = new ResizableModalWindow() {
            @Override
            protected void onShow() {
                initPreferredSize(); // we need to do it after attach to have correct sizes

                super.onShow();
            }
        };
        window.setOuterContentWidget();
        return window;
    }

    @Override
    protected void setContent(Widget widget) {
        contentWidget.setInnerContentWidget(widget);
    }

    private FormContainer prevForm;

    @Override
    public void onAsyncInitialized() {
        contentWidget.show();

        super.onAsyncInitialized();
    }

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
