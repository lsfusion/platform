package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import platform.gwt.view.GPropertyDraw;

public class GTextType extends GDataType {
    public static GType instance = new GTextType();

    @Override
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.TEXT;
    }

    @Override
    public FormItem createFormItem(GPropertyDraw property) {
        TextAreaItem textAreaItem = new TextAreaItem();
        textAreaItem.setTitleOrientation(TitleOrientation.TOP);
        textAreaItem.setEndRow(true);
        textAreaItem.setStartRow(true);
        textAreaItem.setColSpan("*");
        textAreaItem.setWidth("*");
        return textAreaItem;
    }
}
