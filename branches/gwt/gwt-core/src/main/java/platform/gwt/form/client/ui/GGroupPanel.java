package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import platform.gwt.form.client.FormFrame;
import platform.gwt.form.client.utills.GwtUtils;
import platform.gwt.view.GForm;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.HashMap;

public class GGroupPanel extends Canvas {
    private final FormFrame frame;
    private final GForm form;
    private final GGroupObjectController groupController;
    private DynamicForm propertiesForm;

    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
    public HashMap<GPropertyDraw, Object> values = new HashMap<GPropertyDraw, Object>();

    public GGroupPanel(FormFrame iframe, GForm iform, GGroupObjectController igroupController) {
        this.frame = iframe;
        form = iform;
        this.groupController = igroupController;

        propertiesForm = new DynamicForm();
        propertiesForm.setWidth100();
        propertiesForm.setWrapItemTitles(false);

        addChild(propertiesForm);
    }

    public void addProperty(GPropertyDraw property) {
        if (!properties.contains(property)) {
            int ins = GwtUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(ins, property);
        }
    }

    public void removeProperty(GPropertyDraw property) {
        properties.remove(property);
        values.remove(property);
    }

    public void setValue(GPropertyDraw property, HashMap<GGroupObjectValue, Object> valueMap) {
        if (valueMap != null && !valueMap.isEmpty()) {
            values.put(property, valueMap.values().iterator().next());
        }
    }

    public void update() {
        FormItem fields[] = new FormItem[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            GPropertyDraw property = properties.get(i);
            fields[i] = property.createFormItem();
            fields[i].setValue(values.get(property));
        }
        propertiesForm.setFields(fields);

        setVisible(!isEmpty());
    }

    public boolean isEmpty() {
        return properties.size() == 0;
    }
}
