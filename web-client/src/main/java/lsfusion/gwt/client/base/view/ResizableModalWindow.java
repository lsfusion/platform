package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.form.view.ModalForm;

public class ResizableModalWindow extends ModalWindow {

    public ResizableModalWindow(boolean syncType) {
        super(true, syncType, ModalWindowSize.FIT_CONTENT);
    }

    //This scheme is necessary when one modal window is started before the second one, but is displayed later due to delays.
    //In this case, the order in which the windows are displayed must be maintained according to the order of request indexes.
    private FormRequestData formRequestData;

    public void show(FormRequestData formRequestData, Integer insertIndex, PopupOwner popupOwner) {
        this.formRequestData = formRequestData;
        show(insertIndex, popupOwner);
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

}
