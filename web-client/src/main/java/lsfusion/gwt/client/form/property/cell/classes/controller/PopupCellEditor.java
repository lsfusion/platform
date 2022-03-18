package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;

public interface PopupCellEditor extends RequestCellEditor {

    // lack of fields multiple inheritance
    default boolean removeBorder() { return false; }
    void setPopup(PopupDialogPanel popup);
    PopupDialogPanel getPopup();

    default void enterPressed(Element parent) {}

    default boolean commitOnAutoClose() {
        return false;
    }

    default boolean checkEnterEvent(NativeEvent event) {
        return GKeyStroke.isPlainKeyEvent(event);
    }

    class PopupEditorDialogPanel extends PopupDialogPanel {

        private final PopupCellEditor editor;
        private final Element parent;

        public PopupEditorDialogPanel(PopupCellEditor editor, Element parent, boolean removeBorder) {
            this.editor = editor;
            this.parent = parent;

            if(removeBorder)
                getContainerElement().removeClassName("popupContent");
        }

        @Override
        protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
            if (Event.ONKEYUP == event.getTypeInt()) {
                NativeEvent nativeEvent = event.getNativeEvent();
                if (nativeEvent.getKeyCode() == KeyCodes.KEY_ESCAPE) {
                    GwtClientUtils.stopPropagation(nativeEvent);
                    editor.cancel(parent, CancelReason.ESCAPE_PRESSED);
                } else if (nativeEvent.getKeyCode() == KeyCodes.KEY_ENTER && editor.checkEnterEvent(nativeEvent)) {
                    editor.enterPressed(parent);
                }
            }
        }

        @Override
        public void hide(boolean autoClosed) {
            if (autoClosed) {
                if(editor.commitOnAutoClose())
                    editor.commit(parent, CommitReason.FORCED);
                else
                    editor.cancel(parent);
            }
            super.hide(autoClosed);
        }
    }
    @Override
    default void start(Event editEvent, final Element parent, Object oldValue) {
        setPopup(new PopupEditorDialogPanel(this, parent, removeBorder()));
    }

    default void stop(Element parent, boolean cancel) {
        getPopup().hide();
    }
}
