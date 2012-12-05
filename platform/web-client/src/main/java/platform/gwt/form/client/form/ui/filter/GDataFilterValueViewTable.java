package platform.gwt.form.client.form.ui.filter;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.DataGrid;
import platform.gwt.form.client.form.ui.GPropertyTable;
import platform.gwt.form.shared.view.GKeyStroke;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.NativeEditEvent;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.util.Arrays;

import static com.google.gwt.dom.client.BrowserEvents.*;

public class GDataFilterValueViewTable extends DataGrid implements EditManager {
    private GDataFilterValueView valueView;
    private GPropertyDraw property;

    private Object value;

    private DataFilterValueEditableCell cell;

    public interface GDataFilterValueViewTableResource extends Resources {
        @Source("../GSinglePropertyTable.css")
        GDataFilterValueViewTableStyle dataGridStyle();
    }

    public interface GDataFilterValueViewTableStyle extends Style {}

    public static final GDataFilterValueViewTableResource GFILTER_VALUE_TABLE_RESOURCE = GWT.create(GDataFilterValueViewTableResource.class);

    public GDataFilterValueViewTable(GDataFilterValueView valueView, GPropertyDraw property) {
        super(50, GFILTER_VALUE_TABLE_RESOURCE);

        this.valueView = valueView;
        this.property = property;

        setRemoveKeyboardStylesOnFocusLost(true);

        setSize("100%", "100%");
        setTableWidth(property.getPreferredPixelWidth(), com.google.gwt.dom.client.Style.Unit.PX);

        cell = new DataFilterValueEditableCell();

        addColumn(new Column<Object, Object>(cell) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setRowData(Arrays.asList(new Object()));

        setKeyboardSelectionHandler(new GPropertyTable.PropertyTableKeyboardSelectionHandler(this));
    }

    public void setProperty(GPropertyDraw property) {
        this.property = property;
        setTableWidth(property.getPreferredPixelWidth(), com.google.gwt.dom.client.Style.Unit.PX);
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
        redraw();
    }

    public void focusOnValue() {
        setKeyboardSelectedRow(0, true);
        setKeyboardSelectedColumn(0, true);
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

    class DataFilterValueEditableCell extends AbstractCell<Object> {
        private boolean isInEditingState = false;
        private GridCellEditor cellEditor;

        public DataFilterValueEditableCell() {
            super(DBLCLICK, KEYDOWN, KEYPRESS, BLUR);
        }

        @Override
        public void render(Context context, Object value, SafeHtmlBuilder sb) {
            if (isInEditingState) {
                cellEditor.render(context, value, sb);
            } else {
                GridCellRenderer renderer = property.getGridCellRenderer();
                renderer.render(context, value, sb);
            }
        }

        @Override
        public void onBrowserEvent(Context context, Element parent, Object value, NativeEvent event, ValueUpdater<Object> valueUpdater) {
            if ((BrowserEvents.DBLCLICK.equals(event.getType()) || GKeyStroke.isPossibleEditKeyEvent(event)) && cellEditor == null && event.getKeyCode() != KeyCodes.KEY_ESCAPE) {
                startEditing(new NativeEditEvent(event), context, parent);
            }
            if (isInEditingState) {
                cellEditor.onBrowserEvent(context, parent, value, event, valueUpdater);
                if (event.getKeyCode() == KeyCodes.KEY_ENTER && !isInEditingState) {
                    valueView.applyFilter();
                }
            } else {
                super.onBrowserEvent(context, parent, value, event, valueUpdater);
            }
        }

        public void startEditing(EditEvent event, Context context, Element parent) {
            isInEditingState = true;
            cellEditor = property.createValueCellEdtor(GDataFilterValueViewTable.this);
            setValue(context, parent, value);
            cellEditor.startEditing(event, context, parent == null ? getElement().getParentElement() : parent, value);
        }

        public void finishEditing() {
            isInEditingState = false;
            cellEditor = null;
            GDataFilterValueViewTable.this.setValue(value);
        }
    }
}
