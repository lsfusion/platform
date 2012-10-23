package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.changes.dto.ColorDTO;
import platform.gwt.form2.shared.view.grid.GridEditableCell;
import platform.gwt.utils.GwtSharedUtils;

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

        setTableBuilder(new GSinglePropertyTableCellBuilder(this));

        addColumn(new Column<Object, Object>(new GridEditableCell(this)) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setRowData(Arrays.asList(new Object()));
    }

    @Override
    protected boolean isSingleCellTable() {
        return true;
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

    public GPropertyDraw getProperty(int row, int column) {
        assert row == 0 && column == 0;
        return property;
    }

    @Override
    public GGroupObjectValue getColumnKey(int row, int column) {
        return columnKey;
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
