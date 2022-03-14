package lsfusion.gwt.client.form.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableModalWindow;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

public class ModalForm extends FormContainer {

    protected final ResizableModalWindow contentWidget;

    @Override
    public GWindowFormType getWindowType() {
        return GWindowFormType.FLOAT;
    }

    @Override
    protected Element getFocusedElement() {
        return contentWidget.getElement();
    }

    public ModalForm(FormsController formsController, String caption, boolean async, Event editEvent) {
        super(formsController, async, editEvent);

        ResizableModalWindow window = new ResizableModalWindow() {
            @Override
            protected void onShow() {
                initPreferredSize(); // we need to do it after attach to have correct sizes

                super.onShow();
            }
        };
        window.setOuterContentWidget();
        window.setCaption(caption);

        contentWidget = window;
    }

    protected void initPreferredSize() {
        int maxWidth = Window.getClientWidth() - 20;
        int maxHeight = Window.getClientHeight() - 100;

        Dimension size;
        if(async)
            size = new Dimension(Math.min(790, maxWidth), Math.min(580, maxHeight));
        else
            size = form.getPreferredSize(maxWidth, maxHeight);

        contentWidget.setInnerContentSize(size);
    }

    @Override
    protected void setContent(Widget widget) {
        contentWidget.setInnerContentWidget(widget);
    }

    private FormContainer prevForm;

    @Override
    public void onAsyncInitialized() {
        // actually it's already shown, but we want to update preferred sizes after setting the content
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
        if(async)
            contentWidget.focus();
    }

    @Override
    public void hide(EndReason editFormCloseReason) {
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
