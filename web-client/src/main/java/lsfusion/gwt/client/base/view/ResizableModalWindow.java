package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.form.view.ModalForm;

public class ResizableModalWindow extends ModalWindow {

    private final boolean syncType;

    public ResizableModalWindow(boolean syncType) {
        super(true, syncType, ModalWindowSize.FIT_CONTENT);
        this.syncType = syncType;
    }

    public boolean isSyncType() {
        return syncType;
    }

    //This scheme is necessary when one modal window is started before the second one, but is displayed later due to delays.
    //In this case, the order in which the windows are displayed must be maintained according to the order of request indexes.
    // That is for a window waited for: the server runs its opener's later requests only once it is closed. A window not
    // waited for (FLOAT NOWAIT) holds no request back, so a later window of its opener goes on top of it as any other
    private FormRequestData formRequestData;

    public void show(FormRequestData formRequestData, Integer insertIndex, PopupOwner popupOwner) {
        this.formRequestData = formRequestData;
        show(insertIndex, popupOwner);
    }

    public static Pair<ModalForm, Integer> getFormInsertIndex(FormRequestData formRequestData) {
        AbsolutePanel boundaryPanel = RootPanel.get();
        for (int i = 0; i < boundaryPanel.getWidgetCount(); i++) {
            Widget widget = boundaryPanel.getWidget(i);
            if (widget instanceof ResizableModalWindow && ((ResizableModalWindow) widget).syncType) {
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
