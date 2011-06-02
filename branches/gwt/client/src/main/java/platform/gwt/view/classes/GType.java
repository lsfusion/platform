package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.view.GPropertyDraw;

import java.io.Serializable;

public interface GType extends Serializable {
    ListGridFieldType getFieldType();

    ListGridField createGridField(GPropertyDraw property);

    Canvas createCellRenderer(Object value, GPropertyDraw property);

    FormItem createFormItem(GPropertyDraw property);
}
