package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;

public abstract class PopupBasedGridCellEditor extends AbstractGridCellEditor {
    protected final SafeHtmlRenderer<String> renderer = SimpleSafeHtmlRenderer.getInstance();

    private final EditManager editManager;
    private final Style.TextAlign textAlign;

    private final PopupPanel popup;

    public PopupBasedGridCellEditor(EditManager editManager) {
        this(editManager, null);
    }

    public PopupBasedGridCellEditor(EditManager editManager, Style.TextAlign textAlign) {
        this.editManager = editManager;
        this.textAlign = textAlign;

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
    protected void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        String sValue = value == null ? null : renderToString(value);
        renderAligned(sValue, renderer, textAlign, sb);
    }

    protected String renderToString(Object value) {
        return value == null ? "" : value.toString();
    }

    protected void onEscapePressed() {
        cancelEditing();
    }

    protected abstract Widget createPopupComponent();


    interface Template extends SafeHtmlTemplates {
        @Template("<div style=\"text-align: {0};\">{1}</div>")
        SafeHtml aligned(String alignment, SafeHtml text);
    }

    private static Template template;

    public static Template getTemplate() {
        if (template == null) {
            template = GWT.create(Template.class);
        }
        return template;
    }

    public static void renderAligned(String text, SafeHtmlRenderer<String> renderer, Style.TextAlign textAlign, SafeHtmlBuilder sb) {
        if (text == null || text.trim().isEmpty()) {
            sb.appendHtmlConstant("&nbsp;");
        } else {
            SafeHtml safeText = renderer.render(text);

            if (textAlign != null) {
                safeText = getTemplate().aligned(textAlign.getCssName(), safeText);
            }

            sb.append(safeText);
        }
    }
}
