package platform.gwt.view;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.view.classes.GType;

public class GPropertyDraw extends GComponent {
    public int ID;
    public GGroupObject groupObject;
    public String sID;
    public String caption;
    public GType baseType;
    public String iconPath;

    public ListGridField createGridField() {
        return baseType.createGridField(this);
    }

    public Canvas createCellRenderer(Object value) {
        return baseType.createCellRenderer(value, this);
    }

    public FormItem createFormItem() {
        FormItem item = baseType.createFormItem(this);
        item.setTitle(caption != null ? caption : "");
        item.setName(sID);
        return item;
    }
}
