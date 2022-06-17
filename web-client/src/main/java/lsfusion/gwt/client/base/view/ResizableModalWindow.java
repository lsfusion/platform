package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.view.ModalForm;

import java.util.HashMap;
import java.util.Map;

public class ResizableModalWindow extends ResizableWindow {

    private static ModalMask modalMask;
    private static Map<Widget, Pair<ModalForm, Long>> formRequestIndexMap = new HashMap<>();

    public void showModal(Pair<ModalForm, Long> formRequestIndex, Integer insertIndex) {
        showModalMask(formRequestIndex);
        super.show(insertIndex);
    }

    public Pair<ModalForm, Integer> getFormInsertIndex(Long requestIndex) {
        AbsolutePanel boundaryPanel = getBoundaryPanel();
        for(int i = 0; i < boundaryPanel.getWidgetCount(); i++) {
            Widget widget = boundaryPanel.getWidget(i);
            Pair<ModalForm, Long> form = formRequestIndexMap.get(widget);
            if(form != null && form.second != null && form.second < requestIndex) {
                return new Pair(form.first, i);
            }
        }
        return null;
    }

    public void hideModal() {
        super.hide();
        hideModalMask();
    }

    public void showModalMask(Pair<ModalForm, Long> formRequestIndex) {
        if (modalMask == null) {
            modalMask = new ModalMask();
            modalMask.show();
        }
        formRequestIndexMap.put(this, formRequestIndex);
    }

    public void hideModalMask() {
        formRequestIndexMap.remove(this);
        if (modalMask != null && formRequestIndexMap.isEmpty()) {
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
