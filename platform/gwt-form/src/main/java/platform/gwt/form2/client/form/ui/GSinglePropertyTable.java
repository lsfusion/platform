package platform.gwt.form2.client.form.ui;

import com.google.gwt.core.client.GWT;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GridDataRecord;
import platform.gwt.form2.shared.view.changes.dto.ColorDTO;

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
    private GridDataRecord valueRecord;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty) {
        super(iform, GSINGLE_PROPERTY_TABLE_RESOURCE);

        this.property = iproperty;

        addColumn(property.createGridColumn(this, form));
        setRowData(Arrays.asList(new Object()));
    }

    public void setValue(Object value) {
        this.value = value;
        this.valueRecord = new GridDataRecord(property.sID, value);
        setRowData(Arrays.asList(valueRecord));
    }

    public void setBackgroundColor(ColorDTO color) {
        //todo:
    }

    public void setForegroundColor(ColorDTO color) {
        //todo:
    }

    public GPropertyDraw getProperty(int column) {
        assert column == 0;
        return property;
    }

    @Override
    public void setValueAt(int index, int column, Object value) {
        assert index == 0 && column == 0;
        setValue(value);
    }

    @Override
    public Object getValueAt(int index, int column) {
        assert index == 0 && column == 0;
        return value;
    }
}
