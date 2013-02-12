package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;

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
        popup.setWidget(createPopupComponent());
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, final Element parent, Object oldValue) {
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
    public final void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event) {
        //NOP
    }

    @Override
    public void renderDom(Cell.Context context, DivElement cellParent, Object value) {
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
        cancelEditing();
    }

    protected abstract Widget createPopupComponent();
}
