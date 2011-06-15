package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.renderer.FormFieldPropertyRenderer;
import platform.gwt.view.renderer.GPropertyRenderer;

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
    public GPropertyRenderer createPanelRenderer(GPropertyDraw property) {
        // по умолчанию рендеринг через DynamicForm
        FormItem item = createFormItem(property);
        item.setTitle(property.caption != null ? property.caption : "");
        item.setName(property.sID);

        return new FormFieldPropertyRenderer(item);
    }

    @Override
    public FormItem createFormItem(GPropertyDraw property) {
        TextItem textItem = new TextItem();
        textItem.setAttribute("readOnly", true);
        return textItem;
    }
}
