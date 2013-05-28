package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.cellview.client.Column;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.changes.dto.ColorDTO;
import platform.gwt.form.shared.view.grid.GridEditableCell;

import java.text.ParseException;
import java.util.Arrays;

public class GSinglePropertyTable extends GPropertyTable<Object> {
    /**
     * Default style's overrides
     */
    public interface GSinglePropertyTableResource extends Resources {
        @Source("GSinglePropertyTable.css")
        GSinglePropertyTableStyle style();
    }
    public interface GSinglePropertyTableStyle extends Style {}

    public static final GSinglePropertyTableResource GSINGLE_PROPERTY_TABLE_RESOURCE = GWT.create(GSinglePropertyTableResource.class);

    private final GPropertyDraw property;
    private GGroupObjectValue columnKey;
    private Object value;
    private boolean readOnly = false;

    private String background;
    private String foreground;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty, GGroupObjectValue columnKey) {
        super(iform, GSINGLE_PROPERTY_TABLE_RESOURCE, true);

        this.property = iproperty;
        this.columnKey = columnKey;

        setTableBuilder(new GSinglePropertyTableBuilder(this));

        setCellHeight(property.getPreferredPixelHeight());
        setRemoveKeyboardStylesOnBlur(true);

        getTableDataScroller().removeScrollbars();

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

    public void setReadOnly(boolean readOnly) {
        if (this.readOnly != readOnly) {
            this.readOnly = readOnly;
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

    @Override
    public GPropertyDraw getSelectedProperty() {
        return property;
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
        return !readOnly;
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

    @Override
    public void pasteData(String stringValue, boolean multi) {
        try {
            if (stringValue.contains("\n") && (stringValue.indexOf("\n") == stringValue.lastIndexOf("\n") && stringValue.endsWith("\n"))) {
                // при копировании из Excel даже одной ячейки приходит \n в конце строки
                stringValue = stringValue.replace("\n", "");
            }
            Object value = property.parseString(stringValue);
            if (value != null) {
                form.getSimpleDispatcher().changeProperty(value, property, columnKey, true);
            }
        } catch (ParseException ignored) {
        }
    }

    @Override
    public void selectNextCellInColumn(boolean down) {
    }
}
