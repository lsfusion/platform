package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.AbstractGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static java.lang.Math.max;
import static java.lang.Math.min;

public abstract class PopupBasedGridCellEditor extends AbstractGridCellEditor {
    protected final SafeHtmlRenderer<String> renderer = SimpleSafeHtmlRenderer.getInstance();

    protected GPropertyDraw property;

    private final EditManager editManager;
    private final Style.TextAlign textAlign;

    private final PopupPanel popup;

    public PopupBasedGridCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, null);
    }

    public PopupBasedGridCellEditor(EditManager editManager, GPropertyDraw property, Style.TextAlign textAlign) {
        this.editManager = editManager;
        this.textAlign = textAlign;
        this.property = property;

        popup = new PopupPanel(true, true) {
            @Override
            protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                if (Event.ONKEYUP == event.getTypeInt()) {
                    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                        onEscapePressed();
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
        popup.setWidget(createPopupComponent());
    }

    @Override
    public void startEditing(Event editEvent, final Element parent, Object oldValue) {
        showPopup(parent);
    }

    public final void showPopup(final Element parent) {
        if (parent == null) {
            popup.center();
        } else {
            popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
                @Override
                public void setPosition(int offsetWidth, int offsetHeight) {
                    int wndHeight = Window.getClientHeight();
                    int wndWidth =  Window.getClientWidth();
                    int left = max(0, min(parent.getAbsoluteLeft(), wndWidth - offsetWidth));
                    int top = max(0, min(parent.getAbsoluteBottom(), wndHeight - offsetHeight));

                    popup.setPopupPosition(left, top);
                }
            });
        }
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

    @Override
    public void renderDom(Element cellParent, RenderContext renderContext, UpdateContext updateContext) {
        //NOP
    }

    @Override
    public boolean replaceCellRenderer() {
        return false;
    }

    protected String renderToString(Object value) {
        return value == null ? "" : value.toString();
    }

    protected void onEscapePressed() {
        onCancel();
    }

    protected abstract Widget createPopupComponent();
}
