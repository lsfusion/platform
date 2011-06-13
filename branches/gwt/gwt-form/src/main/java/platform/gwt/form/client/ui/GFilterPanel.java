package platform.gwt.form.client.ui;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TileLayoutPolicy;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.tile.TileLayout;

public class GFilterPanel extends TileLayout {
    public GFilterPanel() {
        setHeight(1);
        setLayoutPolicy(TileLayoutPolicy.FLOW);
        setOverflow(Overflow.VISIBLE);
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    private boolean isEmpty = true;
    public void addFilterComponent(FormItem item) {
        DynamicForm itemForm = new DynamicForm();
//        itemForm.setColWidths("0", "*");
//        itemForm.setOverflow(Overflow.VISIBLE);
        itemForm.setWrapItemTitles(false);

        itemForm.setFields(item);

        addTile(itemForm);

        isEmpty = false;
    }
}
