package platform.client.descriptor.editor.base;

import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.base.BaseUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class IncrementListEditor extends JList implements IncrementView, ListSelectionListener {

    public void valueChanged(ListSelectionEvent ev) {
        BaseUtils.invokeSetter(object, field, Arrays.asList(getSelectedValues()));
    }

    private final Object object;
    private final String field;
    public IncrementListEditor(Object object, String field, ListModel dataModel) {
        super(dataModel);

        this.object = object;
        this.field = field;

        addListSelectionListener(this);
        IncrementDependency.add(object, field, this);
    }

    public void update(Object updateObject, String updateField) {
        clearSelection();

        List<?> selected = (List<?>) BaseUtils.invokeGetter(object, field);
        for(Object selectObject : selected)
            setSelectedValue(selectObject, false);
    }
}
