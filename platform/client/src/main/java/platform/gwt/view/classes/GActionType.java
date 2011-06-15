package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import platform.gwt.view.GPropertyDraw;

public class GActionType extends GDataType {
    public static GType instance = new GActionType();

    @Override
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.BOOLEAN;
    }

    @Override
    public FormItem createFormItem(GPropertyDraw property) {
        ButtonItem buttonItem = new ButtonItem();
        buttonItem.setEndRow(false);
        buttonItem.setStartRow(false);
        buttonItem.setIcon(property.iconPath);
        return buttonItem;
    }

    @Override
    public Canvas createCellRenderer(Object value, GPropertyDraw property) {
        final IButton btn = new IButton();
        btn.setTitle(property.iconPath == null ? "..." : "");
        btn.setIcon(property.iconPath);
        btn.setWidth(48);
        btn.setHeight(16);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                //todo:
            }
        });
        if (value == null) {
            btn.disable();
        }

        return btn;
    }
}
