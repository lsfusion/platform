package platform.gwt.view.renderer;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import platform.gwt.view.GPropertyDraw;

public class FormFieldPropertyRenderer implements GPropertyRenderer {
    private FormItem item;
    private DynamicForm itemForm;

    public FormFieldPropertyRenderer(FormItem item) {
        this.item = item;

        itemForm = new DynamicForm();
        itemForm.setOverflow(Overflow.VISIBLE);
        itemForm.setWrapItemTitles(false);

        itemForm.setFields(item);

        itemForm.setAutoWidth();
        itemForm.setAutoHeight();
    }

    @Override
    public Canvas getComponent() {
        return itemForm;
    }

    @Override
    public void setValue(GPropertyDraw property, Object value) {
        item.setValue(value);
    }
}
