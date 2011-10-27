package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.GridDataRecord;
import platform.gwt.view.logics.FormLogicsProvider;
import platform.gwt.view.renderer.FormFieldTypeRenderer;
import platform.gwt.view.renderer.GTypeRenderer;

import java.io.Serializable;

public abstract class GType implements Serializable {
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.TEXT;
    }

    public ListGridField createGridField(FormLogicsProvider formLogics, GPropertyDraw property) {
        ListGridField gridField = new ListGridField(property.sID, property.caption);
        gridField.setType(getFieldType());
        return gridField;
    }

    public FormItem createEditorType(FormLogicsProvider formLogics, GPropertyDraw property) {
        // по умолчанию просто используем тот же FormItem, что и в панели...
        return createPanelFormItem(formLogics, property);
    }

    public Canvas createGridCellRenderer(FormLogicsProvider logics, GGroupObject group, GridDataRecord record, GPropertyDraw property) {
        return null;
    }

    public Canvas updateGridCellRenderer(final Canvas component, final GridDataRecord record) {
        return null;
    }

    public GTypeRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        // по умолчанию рендеринг через FormItem
        FormItem item = property.changeType.createPanelFormItem(formLogics, property);
        item.setAttribute("readOnly",
                          item.getAttributeAsBoolean("readOnly") != null && item.getAttributeAsBoolean("readOnly")
                          || property.readOnly
                          || !formLogics.isEditingEnabled());

        item.setTitle(property.caption != null ? property.caption : "");
        item.setName(property.sID);

        return new FormFieldTypeRenderer(property, item);
    }

    public FormItem createPanelFormItem(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new TextItem();
    }
}
