package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.WindowCellEditor;

import static java.lang.Math.max;
import static java.lang.Math.min;

public abstract class PopupBasedCellEditor implements WindowCellEditor {
    protected final SafeHtmlRenderer<String> renderer = SimpleSafeHtmlRenderer.getInstance();

    protected GPropertyDraw property;

    private final EditManager editManager;
    private final Style.TextAlign textAlign;

    private PopupDialogPanel popup;
    private Widget popupComponent;

    public PopupBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, null);
    }

    public PopupBasedCellEditor(EditManager editManager, GPropertyDraw property, Style.TextAlign textAlign) {
        this.editManager = editManager;
        this.textAlign = textAlign;
        this.property = property;

        popup = new PopupDialogPanel() {
            @Override
            protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                if (Event.ONKEYUP == event.getTypeInt()) {
                    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                        GwtClientUtils.stopPropagation(event.getNativeEvent());
                        onCancel();
                    }
                }
            }

            @Override
            public void hide(boolean autoClosed) {
                if (autoClosed) {
                    cancelEditing();
                }
                super.hide(autoClosed);
            }
        };

        popupComponent = createPopupComponent();
    }

    @Override
    public void startEditing(Event editEvent, final Element parent, Object oldValue) {
        GwtClientUtils.showPopupInWindow(popup, popupComponent, parent.getAbsoluteLeft(), parent.getAbsoluteBottom());
    }

    protected final void commitEditing(Object value) {
        popup.hide();
        editManager.commitEditing(value);
    }

    protected final void cancelEditing() {
        editManager.cancelEditing();
    }
    
    protected final void onCancel() {
        popup.hide();
        cancelEditing();
    }

    protected String renderToString(Object value) {
        return value == null ? "" : value.toString();
    }

    protected abstract Widget createPopupComponent();
}
