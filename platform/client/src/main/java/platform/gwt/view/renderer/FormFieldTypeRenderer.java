package platform.gwt.view.renderer;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.view.GPropertyDraw;

public class FormFieldTypeRenderer implements GTypeRenderer {
    private FormItem item;
    private DynamicForm itemForm;
    private PropertyChangedHandler handler;
    private GPropertyDraw property;

    public FormFieldTypeRenderer(GPropertyDraw property, FormItem item) {
        this.property = property;
        this.item = item;

        itemForm = new DynamicForm();
        itemForm.setAutoWidth();
        itemForm.setAutoHeight();
        itemForm.setOverflow(Overflow.VISIBLE);
        itemForm.setWrapItemTitles(false);

        itemForm.setFields(item);

        item.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                fireValueChaned(event.getValue());
            }
        });
    }

    private void fireValueChaned(Object value) {
        if (handler != null) {
            handler.onChanged(property, value);
        }
    }

    @Override
    public Canvas getComponent() {
        return itemForm;
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String) {
            value = GwtSharedUtils.rtrim((String) value);
        }
        item.setValue(value);
    }

    public void setTitle(Object title) {
        item.setTitle(title == null ? "" : title.toString().trim());
        item.updateState();
    }

    public void setChangedHandler(PropertyChangedHandler handler) {
        this.handler = handler;
    }

    @Override
    public void updateCellBackgroundValue(Object value) {
        if (value != null)
            itemForm.setBackgroundColor((String) value);
    }

    @Override
    public void updateCellForegroundValue(Object value) {
    }



    public DynamicForm getItemForm() {
        return itemForm;
    }
}
