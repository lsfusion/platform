package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.view.GPropertyDraw;

public class GDataType implements GType {
    @Override
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.TEXT;
    }

    @Override
    public ListGridField createGridField(GPropertyDraw property) {
        ListGridField propertyField = new ListGridField(property.sID, property.caption);
        propertyField.setType(getFieldType());
        return propertyField;
    }

    @Override
    public Canvas createCellRenderer(Object value, GPropertyDraw property) {
        return null;
    }

    @Override
    public FormItem createFormItem(GPropertyDraw property) {
        return new TextItem();
    }
}
