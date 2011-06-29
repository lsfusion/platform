package platform.gwt.view.classes;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.GridDataRecord;
import platform.gwt.view.logics.FormLogicsProvider;

public class GActionType extends GDataType {
    public static GType instance = new GActionType();

    @Override
    public ListGridField createGridField(FormLogicsProvider formLogics, GPropertyDraw property) {
        ListGridField field = super.createGridField(formLogics, property);
        field.setAlign(Alignment.CENTER);
        field.setCellAlign(Alignment.CENTER);
        field.setCellFormatter(new CellFormatter() {
            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
//                убираем рендеринг по умолчанию, а то наблюдаются странные эффекты с наложением кнопки на чекбокс
                return null;
            }
        });
        return field;
    }

    @Override
    public FormItem createPanelFormItem(final FormLogicsProvider formLogics, final GPropertyDraw property) {
        ButtonItem buttonItem = new ButtonItem();
        buttonItem.setEndRow(false);
        buttonItem.setStartRow(false);
        buttonItem.setIcon(property.iconPath);
        buttonItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                formLogics.changePropertyDraw(property, true);
            }
        });

        return buttonItem;
    }

    @Override
    public Canvas createGridCellRenderer(final FormLogicsProvider formLogics, final GGroupObject group, final GridDataRecord record, final GPropertyDraw property) {
        final Button btn = new Button();
        btn.setTitle(property.iconPath == null ? "..." : null);
        btn.setIcon(property.iconPath);
        btn.setWidth(48);
        btn.setHeight(18);
        btn.setAlign(Alignment.CENTER);

        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                formLogics.changePropertyDraw(group, record.key, property, true);
            }
        });
        if (record.getAttribute(property.sID) == null) {
            btn.disable();
        }

        return btn;
    }
}
