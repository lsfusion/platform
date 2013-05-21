package platform.gwt.form.client.form.ui.filter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.cellview.client.cell.AbstractCell;
import platform.gwt.form.client.form.ui.CopyPasteUtils;
import platform.gwt.form.client.form.ui.GPropertyTableBuilder;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.NativeEditEvent;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.util.Arrays;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static com.google.gwt.dom.client.Style.Unit;
import static platform.gwt.base.client.GwtClientUtils.removeAllChildren;
import static platform.gwt.base.client.GwtClientUtils.stopPropagation;

public class GDataFilterValueViewTable extends DataGrid implements EditManager {
    private GDataFilterValueView valueView;
    private GPropertyDraw property;

    private Object value;

    private DataFilterValueEditableCell cell;

    public interface GDataFilterValueViewTableResource extends Resources {
        @Source("platform/gwt/form/client/form/ui/GSinglePropertyTable.css")
        GDataFilterValueViewTableStyle style();
    }

    public interface GDataFilterValueViewTableStyle extends Style {
    }

    public static final GDataFilterValueViewTableResource GFILTER_VALUE_TABLE_RESOURCE = GWT.create(GDataFilterValueViewTableResource.class);

    public GDataFilterValueViewTable(GDataFilterValueView valueView, GPropertyDraw property) {
        super(GFILTER_VALUE_TABLE_RESOURCE);

        this.valueView = valueView;
        this.property = property;

        sinkEvents(Event.ONPASTE);

        setRemoveKeyboardStylesOnBlur(true);

        setSize("100%", property.getMinimumHeight());
        setTableWidth(property.getPreferredPixelWidth(), Unit.PX);
        getTableDataScroller().removeScrollbars();

        cell = new DataFilterValueEditableCell();

        addColumn(new Column<Object, Object>(cell) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setTableBuilder(new GPropertyTableBuilder<Object>(this) {
            @Override
            public String getBackground(Object rowValue, int row, int column) {
                return null;
            }

            @Override
            public String getForeground(Object rowValue, int row, int column) {
                return null;
            }
        });
        setRowData(Arrays.asList(new Object()));
    }

    public void setProperty(GPropertyDraw property) {
        this.property = property;

        int minimumPixelHeight = property.getMinimumPixelHeight();

        setTableWidth(property.getPreferredPixelWidth(), Unit.PX);
        setHeight(property.getMinimumHeight());

        setCellHeight(minimumPixelHeight);
    }

    private void setCellHeight(int cellHeight) {
        ((GPropertyTableBuilder)getTableBuilder()).setCellHeight(cellHeight);
        setRowHeight(cellHeight + 1); //1px for border
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
        cell.update();
    }

    public void focusOnValue() {
        setFocus(true);
    }

    @Override
    public void commitEditing(Object value) {
        cell.finishEditing();
        valueView.valueChanged(value);
    }

    @Override
    public void cancelEditing() {
        cell.finishEditing();
    }

    @Override
    public void selectNextCellInColumn(boolean down) {
    }

    public void startEditing(EditEvent event) {
        cell.beginEditing(event);
    }

    @Override
    protected void onBrowserEvent2(Event event) {
        if (cell.cellEditor == null && event.getTypeInt() == Event.ONPASTE) { // пока работает только для Chrome
            executePaste(event);
        } else {
            super.onBrowserEvent2(event);
        }
    }

    private void executePaste(Event event) {
        String line = CopyPasteUtils.getClipboardData(event);
//        String line = getClipboardData(event);
        if (!line.isEmpty()) {
            stopPropagation(event);
            line = line.replaceAll("\r\n", "\n");    // браузеры заменяют разделители строк на "\r\n"
//            boolean isMultiLine = line.contains("\n") && (line.indexOf("\n") != line.lastIndexOf("\n") || !line.endsWith("\n"));
//            pasteData(line, line.contains("\t") || isMultiLine);

        }
    }

    class DataFilterValueEditableCell extends AbstractCell<Object> {
        private boolean isInEditingState = false;
        private GridCellEditor cellEditor;
        private DivElement parentElement;
        private Context context;

        public DataFilterValueEditableCell() {
            super(DBLCLICK, KEYDOWN, KEYPRESS, BLUR);
        }

        @Override
        public boolean isEditing(Context context, Element parent, Object value) {
            return isInEditingState;
        }

        @Override
        public void renderDom(Context context, DivElement cellElement, Object value) {
            assert !isInEditingState;
            if (parentElement == null) {
                parentElement = cellElement;
            }
            if (this.context == null) {
                this.context = context;
            }

            GridCellRenderer cellRenderer = property.getGridCellRenderer();
            cellRenderer.renderDom(context, cellElement, value);
        }

        @Override
        public void updateDom(Context context, DivElement cellElement, Object value) {
            assert !isInEditingState;

            GridCellRenderer cellRenderer = property.getGridCellRenderer();
            cellRenderer.updateDom(cellElement, context, value);
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, Object value, NativeEvent event) {
            if ((BrowserEvents.DBLCLICK.equals(event.getType()) || GKeyStroke.isPossibleEditKeyEvent(event) &&
                    !event.getCtrlKey() && !event.getAltKey() && !event.getMetaKey()) &&
                    cellEditor == null &&
                    event.getKeyCode() != KeyCodes.KEY_ESCAPE &&
                    event.getKeyCode() != KeyCodes.KEY_ENTER) {
                startEditing(new NativeEditEvent(event), context, parent);
            }
            if (isInEditingState) {
                cellEditor.onBrowserEvent(context, parent, value, event);
            }
            if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
                valueView.applyFilter();
            }
            if (GKeyStroke.isCopyToClipboardEvent(event)) {
                CopyPasteUtils.putIntoClipboard(parent);
            } else if (GKeyStroke.isPasteFromClipboardEvent(event)) {  // для IE, в котором не удалось словить ONPASTE, но он и так даёт доступ к буферу обмена
                executePaste((Event) event);
            }
        }

        public void beginEditing(final EditEvent event) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    startEditing(event, null, parentElement);
                }
            });
        }

        public void startEditing(EditEvent event, Context context, Element parent) {
            isInEditingState = true;
            cellEditor = property.createValueCellEdtor(GDataFilterValueViewTable.this);

            removeAllChildren(parent);

            cellEditor.renderDom(context, parent.<DivElement>cast(), value);
            cellEditor.startEditing(event, context, parent == null ? getElement().getParentElement() : parent, value);
        }

        public void finishEditing() {
            isInEditingState = false;
            cellEditor = null;
            update();
        }

        public void update() {
            if (context != null && parentElement != null) {
                removeAllChildren(parentElement);
                renderDom(context, parentElement, value);
                redraw();
                focusOnValue();
            }
        }
    }
}
