package lsfusion.gwt.client.form.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FormRequestData;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.base.view.ResizableModalWindow;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

public class ModalForm extends FormContainer {

    protected final ResizableModalWindow contentWidget;
    protected final PopupOwner popupOwner;

    @Override
    public GWindowFormType getWindowType() {
        return GModalityWindowFormType.FLOAT;
    }

    @Override
    public Element getContentElement() {
        return contentWidget.getElement();
    }

    public ModalForm(FormsController formsController, GFormController contextForm, boolean async, boolean syncType, Event editEvent, PopupOwner popupOwner) {
        super(formsController, contextForm, async, editEvent);

        this.popupOwner = popupOwner;
        ResizableModalWindow window = new ResizableModalWindow(syncType) {
            @Override
            public void onShow() {
                initPreferredSize(); // we need to do it after attach to have correct sizes

                super.onShow();
            }
        };

        window.makeShadowOnScroll();

        contentWidget = window;

        // this is form container, that is shrinked and needs padding
        //GwtClientUtils.addXStyleName(contentWidget.getBody(), "form-shrink-padded-container");
    }

    protected void initPreferredSize() {
        if(!async) {
            GSize maxWidth = GwtClientUtils.getOffsetWidth(Document.get().getBody()).subtract(GSize.CONST(20));
            GSize maxHeight = GwtClientUtils.getOffsetHeight(Document.get().getBody()).subtract(GSize.CONST(100));

            form.initPreferredSize(contentWidget.getBody(), maxWidth, maxHeight);
        }
    }

    @Override
    protected void setContent(Widget widget) {
        contentWidget.setBodyWidget(widget);
    }

    private FormContainer prevForm;

    @Override
    public void onAsyncInitialized() {
        // actually it's already shown, but we want to update preferred sizes after setting the content
        contentWidget.onShow();

        super.onAsyncInitialized();
    }

    @Override
    public void show(GAsyncFormController asyncFormController) {
        FormRequestData formRequestData = new FormRequestData(asyncFormController.getDispatcher(), this, asyncFormController.getEditRequestIndex());

        PopupOwner popupOwner = this.popupOwner;
        Pair<ModalForm, Integer> formInsertIndex = ResizableModalWindow.getFormInsertIndex(formRequestData);
        if(formInsertIndex == null) {
            prevForm = MainFrame.getAssertCurrentForm();
            if (prevForm != null) // if there were no currentForm
                prevForm.onBlur(false);
        } else {
            prevForm = formInsertIndex.first.prevForm;
            formInsertIndex.first.prevForm = this;
        }

        contentWidget.show(formRequestData, formInsertIndex != null ? formInsertIndex.second : null, popupOwner);

        if(formInsertIndex == null) {
            onFocus(true);
            if(async)
                focus();
        }
    }

    @Override
    public void hide(EndReason editFormCloseReason) {
        onBlur(true);

        // we need it before to avoid focus going to the body
        if(prevForm != null)
            prevForm.onFocus(false);

        contentWidget.hide();
    }

    @Override
    public Widget getCaptionWidget() {
        return contentWidget.getTitleWidget();
    }
}
