package platform.gwt.view.classes;

import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.logics.FormLogicsProvider;

public class GTextType extends GDataType {
    public static GType instance = new GTextType();

    @Override
    public FormItem createPanelFormItem(FormLogicsProvider formLogics, GPropertyDraw property) {
        TextAreaItem textAreaItem = new TextAreaItem();
        textAreaItem.setWrapTitle(false);
        textAreaItem.setTitleOrientation(TitleOrientation.TOP);
        textAreaItem.setEndRow(true);
        textAreaItem.setStartRow(true);
        textAreaItem.setColSpan("*");
        textAreaItem.setWidth("300");
        return textAreaItem;
    }
}
