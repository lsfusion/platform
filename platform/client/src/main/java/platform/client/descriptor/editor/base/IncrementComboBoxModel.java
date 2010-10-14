package platform.client.descriptor.editor.base;

import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.increment.IncrementDependency;
import platform.base.BaseUtils;

import javax.swing.*;
import java.lang.reflect.Method;

public abstract class IncrementComboBoxModel extends IncrementListModel implements ComboBoxModel {

    public Object selected;

    public final Object object;
    public final String field;

    public void setSelectedItem(Object anItem) {
        if(!BaseUtils.nullEquals(selected, anItem))
            BaseUtils.invokeSetter(object, field, anItem);
    }

    public Object getSelectedItem() {
        return selected;
    }

    private class SelectIncrement implements IncrementView {
        
        public void update(Object updateObject, String updateField) {
            selected = BaseUtils.invokeGetter(object, field);

            fireContentsChanged(this, -1, -1);
        }
    }
    public final SelectIncrement selectIncrement = new SelectIncrement();

    protected IncrementComboBoxModel(Object object, String field) {
        super();
        
        this.object = object;
        this.field = field;

        IncrementDependency.add(object, field, selectIncrement);
    }
}
