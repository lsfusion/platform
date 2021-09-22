package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.WindowValueCellEditor;

public abstract class PopupBasedCellEditor extends WindowValueCellEditor {
    protected final SafeHtmlRenderer<String> renderer = SimpleSafeHtmlRenderer.getInstance();

    protected GPropertyDraw property;
    protected PopupDialogPanel popup;

    public PopupBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager);
        this.property = property;
    }

    @Override
    public void start(Event editEvent, final Element parent, Object oldValue) {
        popup = new PopupDialogPanel() {
            @Override
            protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                if (Event.ONKEYUP == event.getTypeInt()) {
                    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                        GwtClientUtils.stopPropagation(event.getNativeEvent());
                        cancel(parent);
                    }
                }
            }

            @Override
            public void hide(boolean autoClosed) {
                if (autoClosed) {
                    cancel(parent);
                }
                super.hide(autoClosed);
            }
        };

        GwtClientUtils.showPopupInWindow(popup, createPopupComponent(parent), parent.getAbsoluteLeft(), parent.getAbsoluteBottom());
    }

    @Override
    public void onBeforeFinish(Element parent, boolean cancel) {
        popup.hide();
    }

    protected abstract Widget createPopupComponent(Element parent);
}
