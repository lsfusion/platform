package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.form.view.ModalForm;

public class ResizableModalWindow extends ModalWindow {

    public ResizableModalWindow() {
        super(true, ModalWindowSize.FIT_CONTENT);
    }

    //This scheme is necessary when one modal window is started before the second one, but is displayed later due to delays.
    //In this case, the order in which the windows are displayed must be maintained according to the order of request indexes.
    private FormRequestData formRequestData;

    public void show(FormRequestData formRequestData, Integer insertIndex, Widget popupOwnerWidget) {
        this.formRequestData = formRequestData;
        show(insertIndex, popupOwnerWidget);
    }

    public static Pair<ModalForm, Integer> getFormInsertIndex(FormRequestData formRequestData) {
        AbsolutePanel boundaryPanel = RootPanel.get();
        for (int i = 0; i < boundaryPanel.getWidgetCount(); i++) {
            Widget widget = boundaryPanel.getWidget(i);
            if (widget instanceof ResizableModalWindow) {
                FormRequestData widgetData = ((ResizableModalWindow) widget).formRequestData;
                if (widgetData.isBefore(formRequestData)) {
                    return new Pair(widgetData.modalForm, i);
                }
            }
        }
        return null;
    }

    public void hide() {
        super.hide();
        formRequestData = null;
    }

    public void setContentSize(Dimension size) {
        Element element = getContentWidget().getElement();
        FlexPanel.setPrefWidth(element, size.width);
        FlexPanel.setPrefHeight(element, size.height);
    }

    public void focus() {
        Element element = RootPanel.get().getElement();
        int tabIndex = element.getTabIndex();
        if (tabIndex == -1) {
            element.setTabIndex(0);
        }
        FocusUtils.focus(element, FocusUtils.Reason.SHOW);
    }
}
