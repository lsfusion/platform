package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.view.ModalForm;

public class ResizableModalWindow extends ResizableWindow {

    private static ModalMask modalMask;
    private static int modalWindowCount;
    //This scheme is necessary when one modal window is started before the second one, but is displayed later due to delays.
    //In this case, the order in which the windows are displayed must be maintained according to the order of request indexes.
    private FormRequestData formRequestData;

    public void show(FormRequestData formRequestData, Integer insertIndex) {
        modalWindowCount++;
        if (modalMask == null) {
            modalMask = new ModalMask();
            modalMask.show();
        }
        this.formRequestData = formRequestData;
        super.show(insertIndex);
    }

    public Pair<ModalForm, Integer> getFormInsertIndex(FormRequestData formRequestData) {
        AbsolutePanel boundaryPanel = getBoundaryPanel();
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
        modalWindowCount--;
        if (modalMask != null && modalWindowCount == 0) {
            modalMask.hide();
            modalMask = null;
        }
    }

    private final static class ModalMask {
        private final PopupPanel popup;

        private ModalMask() {
            popup = new PopupPanel();
            popup.setGlassEnabled(true);
            popup.getElement().getStyle().setOpacity(0);
        }

        public void show() {
            popup.center();
        }

        public void hide() {
            popup.hide();
        }
    }
}
