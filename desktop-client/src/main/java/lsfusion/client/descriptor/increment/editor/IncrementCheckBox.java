package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.context.*;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class IncrementCheckBox extends JCheckBox implements IncrementView, ItemListener {

    private final Object object;
    private final String field;
    
    public IncrementCheckBox(String title, ApplicationContextProvider object, String field) {
        super(title);
        this.object = object;
        this.field = field;

        addItemListener(this);

        object.getContext().addDependency(object, field, this);
    }

    public void itemStateChanged(ItemEvent e) {
        ReflectionUtils.invokeSetter(object, field, isSelected());
    }

    public void update(Object updateObject, String updateField) {
        setSelected((Boolean) ReflectionUtils.invokeGetter(object, field));
    }
}
