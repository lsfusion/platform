package platform.gwt.form.client.form.ui;

import com.google.gwt.cell.client.AbstractSafeHtmlCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.Header;

import static com.google.gwt.dom.client.Style.Cursor;
import static com.google.gwt.user.client.Event.NativePreviewEvent;
import static com.google.gwt.user.client.Event.NativePreviewHandler;

public class GGridPropertyTableHeader extends Header<String> {
    private static final int ANCHOR_WIDTH = 10;

    private final GGridPropertyTable table;
    private String caption;

    private ColumnResizeHelper resizeHelper = null;

    public GGridPropertyTableHeader(GGridPropertyTable table) {
        this(table, null);
    }

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption) {
        super(new GGridHeaderCell());
        this.caption = caption;
        this.table = table;
    }

    @Override
    public String getValue() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public void onBrowserEvent(Cell.Context context, Element target, NativeEvent event) {
        String eventType = event.getType();
        if ("dblclick".equals(eventType)) {
            table.headerClicked(this, event.getCtrlKey());
        } else if ("mousemove".equals(eventType) || "mousedown".equals(eventType)) {
            if (resizeHelper == null) {
                int mouseX = event.getClientX();

                int anchorRight = target.getAbsoluteRight() - ANCHOR_WIDTH;
                int anchorLeft = target.getAbsoluteLeft() + ANCHOR_WIDTH;

                int headerIndex = table.getHeaderIndex(this);
                if ((mouseX > anchorRight && headerIndex != table.getColumnCount() - 1) || (mouseX < anchorLeft && headerIndex > 0)) {
                    target.getStyle().setCursor(Cursor.COL_RESIZE);
                    if (eventType.equals("mousedown")) {
                        Column leftColumn;
                        Column rightColumn;
                        int initialMouseX;
                        int scaleWidth;
                        int scalePixelWidth = target.getOffsetWidth();
                        if (mouseX > anchorRight) {
                            leftColumn = table.getColumn(headerIndex);
                            rightColumn = table.getColumn(headerIndex + 1);
                            initialMouseX = target.getAbsoluteRight();
                            scaleWidth = getColumnWidth(leftColumn);
                        } else {
                            leftColumn = table.getColumn(headerIndex - 1);
                            rightColumn = table.getColumn(headerIndex);
                            initialMouseX = target.getAbsoluteLeft();
                            scaleWidth = getColumnWidth(rightColumn);
                        }
                        resizeHelper = new ColumnResizeHelper(leftColumn, rightColumn, initialMouseX, scaleWidth, scalePixelWidth);
                        event.preventDefault();
                        event.stopPropagation();
                    }
                } else {
                    target.getStyle().setCursor(Cursor.DEFAULT);
                }
            }
        }
    }

    private int getColumnWidth(Column column) {
        String width = table.getColumnWidth(column);
        return Integer.parseInt(width.substring(0, width.indexOf("px")));
    }

    @Override
    public void render(Cell.Context context, SafeHtmlBuilder sb) {
        Boolean sortDir = table.getSortDirection(this);
        ((GGridHeaderCell)getCell()).render(getValue(), sortDir, sb);
    }

    private class ColumnResizeHelper implements NativePreviewHandler {
        private HandlerRegistration previewHandlerReg;

        private int initalMouseX;

        private Column leftColumn;
        private Column rightColumn;

        private int scaleWidth;
        private int scalePixelWidth;

        private int leftInitialWidth;
        private int rightInitialWidth;

        public ColumnResizeHelper(Column leftColumn, Column rightColumn, int initalMouseX, int scaleWidth, int scalePixelWidth) {
            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;
            this.initalMouseX = initalMouseX;
            this.scaleWidth = scaleWidth;
            this.scalePixelWidth = scalePixelWidth;

            leftInitialWidth = getColumnWidth(leftColumn);
            rightInitialWidth = getColumnWidth(rightColumn);

            previewHandlerReg = Event.addNativePreviewHandler(this);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            NativeEvent nativeEvent = event.getNativeEvent();
            nativeEvent.preventDefault();
            nativeEvent.stopPropagation();

            if (nativeEvent.getType().equals("mousemove")) {
                resizeHeaders(nativeEvent.getClientX());
            } else if (nativeEvent.getType().equals("mouseup")) {
                previewHandlerReg.removeHandler();
                resizeHelper = null;
            }
        }

        private void resizeHeaders(int clientX) {
            int dragX = clientX - initalMouseX;

            int dragColumnWidth = dragX * scaleWidth / scalePixelWidth;

            if (leftInitialWidth + dragColumnWidth > 0 && rightInitialWidth - dragColumnWidth > 0) {
                table.setColumnWidth(leftColumn, (leftInitialWidth + dragColumnWidth) + "px");
                table.setColumnWidth(rightColumn, (rightInitialWidth - dragColumnWidth) + "px");
            }
        }
    }

    public static class GGridHeaderCell extends AbstractSafeHtmlCell<String> {
        interface Template extends SafeHtmlTemplates {
            @Template("<div title=\"{2}\">" +
                              "<img style=\"float:left; height:15px; width: 15px;\" src=\"{1}\"/>" +
                              "<span style=\"white-space:normal;\">{0}</span>" +
                              "</div>"
            )
            SafeHtml arrow(SafeHtml caption, SafeUri icon, String toolTip);

            @Template("<div style=\"white-space:normal;\" title=\"{1}\">{0}</div>")
            SafeHtml textWithTooltip(SafeHtml caption, String toolTip);
        }

        private static Template template;

        public GGridHeaderCell() {
            super(SimpleSafeHtmlRenderer.getInstance(), "dblclick", "mousedown", "mousemove");
            if (template == null) {
                template = GWT.create(Template.class);
            }
        }

        @Override
        protected void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
            //shouldn't be called
            assert false;
        }

        public void render(String value, Boolean sortDir, SafeHtmlBuilder sb) {
            SafeHtml safeValue = getRenderer().render(value);
            value = safeValue.asString();
            if (sortDir != null) {
                sb.append(template.arrow(safeValue, UriUtils.fromString(GWT.getModuleBaseURL() + "images/" + (sortDir ? "arrowup.png" : "arrowdown.png")), value));
            } else {
                sb.append(template.textWithTooltip(safeValue, value));
            }
        }
    }
}