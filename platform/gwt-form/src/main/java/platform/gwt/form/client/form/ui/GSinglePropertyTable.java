package platform.gwt.form.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import platform.gwt.cellview.client.Column;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.changes.dto.ColorDTO;
import platform.gwt.form.shared.view.grid.GridEditableCell;
import platform.gwt.base.shared.GwtSharedUtils;

import java.util.Arrays;

public class GSinglePropertyTable extends GPropertyTable {
    private String background;
    private String foreground;

    /**
     * Default style's overrides
     */
    public interface GSinglePropertyTableResource extends Resources {
        @Source("GSinglePropertyTable.css")
        GSinglePropertyTableStyle dataGridStyle();
    }
    public interface GSinglePropertyTableStyle extends Style {}

    public static final GSinglePropertyTableResource GSINGLE_PROPERTY_TABLE_RESOURCE = GWT.create(GSinglePropertyTableResource.class);

    private final GPropertyDraw property;
    private GGroupObjectValue columnKey;
    private Object value;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty, GGroupObjectValue columnKey) {
        super(iform, GSINGLE_PROPERTY_TABLE_RESOURCE);

        this.property = iproperty;
        this.columnKey = columnKey;

        setRemoveKeyboardStylesOnFocusLost(true);

        setTableBuilder(new GSinglePropertyTableCellBuilder(this));

        addColumn(new Column<Object, Object>(new GridEditableCell(this)) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setRowData(Arrays.asList(new Object()));
    }

    public void setValue(Object value) {
        if (!GwtSharedUtils.nullEquals(this.value, value)) {
            this.value = value;
            redraw();
        }
    }

    public void setBackground(ColorDTO background) {
        String sBackground = background == null ? null : background.toString();
        if (!GwtSharedUtils.nullEquals(this.background, sBackground)) {
            this.background = sBackground;
            redraw();
        }
    }

    public void setForeground(ColorDTO foreground) {
        String sForeground = foreground == null ? null : foreground.toString();
        if (!GwtSharedUtils.nullEquals(this.foreground, sForeground)) {
            this.foreground = sForeground;
            redraw();
        }
    }

    public String getBackground() {
        return background;
    }

    public String getForeground() {
        return foreground;
    }

    public GPropertyDraw getProperty(Cell.Context context) {
        assert context.getIndex() == 0 && context.getColumn() == 0;
        return property;
    }

    @Override
    public GGroupObjectValue getColumnKey(Cell.Context context) {
        return columnKey;
    }

    @Override
    public boolean isEditable(Cell.Context context) {
        return true;
    }

    @Override
    public void setValueAt(Cell.Context context, Object value) {
        assert context.getIndex() == 0 && context.getColumn() == 0;
        setValue(value);
    }

    @Override
    public Object getValueAt(Cell.Context context) {
        assert context.getIndex() == 0 && context.getColumn() == 0;
        return value;
    }
}
