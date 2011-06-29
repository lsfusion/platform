package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.form.fields.BooleanItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.logics.FormLogicsProvider;

public class GLogicalType extends GDataType {
    public static GType instance = new GLogicalType();

    @Override
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.BOOLEAN;
    }

    @Override
    public FormItem createPanelFormItem(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new BooleanItem();
    }
}
