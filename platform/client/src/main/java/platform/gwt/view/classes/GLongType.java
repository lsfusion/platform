package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.logics.FormLogicsProvider;

public class GLongType extends GIntegralType {
    public static GType instance = new GLongType();

    @Override
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.INTEGER;
    }

    @Override
    public FormItem createPanelFormItem(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new IntegerItem();
    }
}
