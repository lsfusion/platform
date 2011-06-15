package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.form.fields.DateItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.ui.DateCellFormatter;
import platform.gwt.view.GPropertyDraw;

public class GDateType extends GDataType {
    public static GType instance = new GDateType();

    @Override
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.DATE;
    }

    @Override
    public ListGridField createGridField(GPropertyDraw property) {
        ListGridField propertyField = super.createGridField(property);
        propertyField.setCellFormatter(DateCellFormatter.instance);
        return propertyField;
    }

    @Override
    public FormItem createFormItem(GPropertyDraw property) {
        return new DateItem();
    }
}
