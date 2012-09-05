package platform.gwt.form2.shared.view.grid.editor;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.shared.view.grid.EditManager;

public abstract class PopupBasedGridEditor implements GridCellEditor {
    protected final SafeHtmlRenderer<String> renderer = SimpleSafeHtmlRenderer.getInstance();

    private final EditManager editManager;

    private final PopupPanel popup;

    public PopupBasedGridEditor(EditManager editManager) {
        this.editManager = editManager;

        popup = new PopupPanel(false, true) {
            @Override
            protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                if (Event.ONKEYUP == event.getTypeInt()) {
                    if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                        onEscapePressed();
                    }
                }
            }
        };
        popup.add(createPopupComponent());
    }

    @Override
    public final void startEditing(NativeEvent editEvent, Cell.Context context, final Element parent) {
        showPopup(parent);
    }

    public final void showPopup(final Element parent) {
        if (parent == null) {
            popup.center();
        } else {
            popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
                @Override
                public void setPosition(int offsetWidth, int offsetHeight) {
                    popup.setPopupPosition(parent.getAbsoluteLeft(), parent.getAbsoluteBottom());
                }
            });
        }
    }

    protected final void commitEditing(Object value) {
        popup.hide();
        editManager.commitEditing(value);
    }

    protected final void cancelEditing() {
        popup.hide();
        editManager.cancelEditing();
    }

    @Override
    public final void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event, ValueUpdater<Object> valueUpdater) {
        //NOP
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        if (value != null) {
            sb.append(renderer.render(formatValue(value)));
        }
    }

    @Override
    public final boolean resetFocus(Cell.Context context, Element parent, Object value) {
        return false;
    }

    protected String formatValue(Object value) {
        return value == null ? "" : value.toString();
    }

    protected void onEscapePressed() {
        cancelEditing();
    }

    protected abstract Widget createPopupComponent();
}
