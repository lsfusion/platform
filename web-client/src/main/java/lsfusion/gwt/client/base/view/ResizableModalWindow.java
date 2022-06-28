package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.form.view.ModalForm;

import java.util.HashMap;
import java.util.Map;

public class ResizableModalWindow extends ResizableWindow {

    private static ModalMask modalMask;
    //This scheme is necessary when one modal window is started before the second one, but is displayed later due to delays.
    //In this case, the order in which the windows are displayed must be maintained according to the order of request indexes.
    private static Map<Widget, FormRequestData> formRequestDataMap = new HashMap<>();

    public void show(FormRequestData formRequestIndex, Integer insertIndex) {
        showModalMask(formRequestIndex);
        super.show(insertIndex);
    }

    public Pair<ModalForm, Integer> getFormInsertIndex(GwtActionDispatcher dispatcher, Long requestIndex) {
        AbsolutePanel boundaryPanel = getBoundaryPanel();
        for(int i = 0; i < boundaryPanel.getWidgetCount(); i++) {
            Widget widget = boundaryPanel.getWidget(i);
            FormRequestData data = formRequestDataMap.get(widget);
            if(data != null && data.dispatcher != null && data.dispatcher.equals(dispatcher) && data.requestIndex != null && data.requestIndex < requestIndex) {
                return new Pair(data.modalForm, i);
            }
        }
        return null;
    }

    public void hide() {
        super.hide();
        hideModalMask();
    }

    public void showModalMask(FormRequestData formRequestData) {
        if (modalMask == null) {
            modalMask = new ModalMask();
            modalMask.show();
        }
        formRequestDataMap.put(this, formRequestData);
    }

    public void hideModalMask() {
        formRequestDataMap.remove(this);
        if (modalMask != null && formRequestDataMap.isEmpty()) {
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
