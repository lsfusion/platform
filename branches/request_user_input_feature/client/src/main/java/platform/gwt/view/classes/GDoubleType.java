package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.form.fields.FloatItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.logics.FormLogicsProvider;

public class GDoubleType extends GIntegralType {
    public static GType instance = new GDoubleType();

    @Override
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.FLOAT;
    }

    @Override
    public FormItem createPanelFormItem(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new FloatItem();
    }
}
