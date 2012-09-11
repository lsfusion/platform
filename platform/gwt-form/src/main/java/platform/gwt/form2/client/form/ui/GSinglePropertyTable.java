package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.changes.dto.ColorDTO;
import platform.gwt.form2.shared.view.grid.GridEditableCell;

import java.util.Arrays;

public class GSinglePropertyTable extends GPropertyTable {
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
    private Object value;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty) {
        super(iform, GSINGLE_PROPERTY_TABLE_RESOURCE);

        this.property = iproperty;

        addColumn(new Column<Object, Object>(new GridEditableCell(this)) {
            @Override
            public Object getValue(Object record) {
                return value;
            }
        });
        setRowData(Arrays.asList(new Object()));
    }

    public void setValue(Object value) {
        this.value = value;
        redraw();
    }

    public void setBackgroundColor(ColorDTO color) {
        //todo:
    }

    public void setForegroundColor(ColorDTO color) {
        //todo:
    }

    public GPropertyDraw getProperty(int row, int column) {
        assert row == 0 && column == 0;
        return property;
    }

    @Override
    public GGroupObjectValue getColumnKey(int row, int column) {
        return null;
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
