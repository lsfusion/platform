package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.layout.HStack;

import java.util.LinkedHashSet;
import java.util.Set;

public class GToolbarPanel extends HStack {
    private Set<Canvas> components = new LinkedHashSet<Canvas>();
    
    public GToolbarPanel() {
        super(10);
        setAutoHeight();
        setBackgroundColor("#F5F5F5");
    }

    public boolean isEmpty() {
        return components.isEmpty();
    }

    public void addComponent(FormItem item) {
        DynamicForm itemForm = new DynamicForm();
        itemForm.setWrapItemTitles(false);

        itemForm.setFields(item);

        addComponent(itemForm);
    }

    public void addComponent(Canvas item) {
        if (components.contains(item))
            removeMember(item);
            addMember(item);
            components.add(item);
    }

    public void removeComponent(Canvas child) {
        if (components.contains(child)) {
            components.remove(child);
            removeMember(child);
        }
    }
    
    public Set<Canvas> getComponents() {
        return components;
    }
}
