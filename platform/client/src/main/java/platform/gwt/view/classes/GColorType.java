package platform.gwt.view.classes;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ColorPickerItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.GridDataRecord;
import platform.gwt.view.logics.FormLogicsProvider;

public class GColorType extends GDataType {
    public static GType instance = new GColorType();

    @Override
    public FormItem createPanelFormItem(FormLogicsProvider formLogics, GPropertyDraw property) {
        ColorPickerItem picker = new ColorPickerItem();
        return picker;
    }

    @Override
    public ListGridField createGridField(FormLogicsProvider formLogics, GPropertyDraw property) {
        ListGridField field = super.createGridField(formLogics, property);
        field.setAlign(Alignment.CENTER);
        field.setCellAlign(Alignment.CENTER);
        field.setCellFormatter(new CellFormatter() {
            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return null;
            }
        });
        return field;
    }

    @Override
    public Canvas createGridCellRenderer(FormLogicsProvider logics, GGroupObject group, GridDataRecord record, GPropertyDraw property) {
        return new ColorRenderer(record, property);
    }

    @Override
    public Canvas updateGridCellRenderer(Canvas component, GridDataRecord record) {
        ColorRenderer renderer = (ColorRenderer) component;
        renderer.setValue(record);
        return renderer;
    }

    private class ColorRenderer extends DynamicForm {
        String propertySID;
        ColorPickerItem colorPicker;

        public ColorRenderer(final GridDataRecord irecord, final GPropertyDraw property) {
            propertySID = property.sID;
            colorPicker = new ColorPickerItem();
            colorPicker.setShowTitle(false);
            setFields(colorPicker);
            setValue(irecord);
        }

        public void setValue(GridDataRecord record) {
            Object value = record.getAttribute(propertySID);
            colorPicker.setValue(value);
        }
    }
}
