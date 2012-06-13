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
                formLogics.executeEditAction(property, "change");
            }
        });

        return buttonItem;
    }

    @Override
    public Canvas createGridCellRenderer(final FormLogicsProvider formLogics, final GGroupObject group, final GridDataRecord record, final GPropertyDraw property) {
        return new ActionButton(formLogics, group, record, property);
    }

    @Override
    public Canvas updateGridCellRenderer(final Canvas component, final GridDataRecord record) {
        ActionButton btn = (ActionButton) component;
        btn.setRecord(record);
        return btn;
    }

    private static class ActionButton extends Button {
        private GridDataRecord record;
        private String propertySID;

        public ActionButton(final FormLogicsProvider formLogics, final GGroupObject group, final GridDataRecord irecord, final GPropertyDraw property) {
            propertySID = property.sID;

            setRecord(irecord);

            setTitle(property.iconPath == null ? "..." : null);
            setIcon(property.iconPath);
            setWidth(48);
            setHeight(18);
            setAlign(Alignment.CENTER);

            addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    formLogics.executeEditAction(property, record.key, "change");
                }
            });
        }

        public void setRecord(GridDataRecord irecord) {
            record = irecord;
            if (record.getAttribute(propertySID) == null) {
                disable();
            }
        }
    }
}
