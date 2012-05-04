package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.base.context.ApplicationContextProvider;
import platform.base.context.IncrementView;
import platform.client.descriptor.editor.base.TristateCheckBox;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class IncrementTristateCheckBox extends TristateCheckBox implements IncrementView, ItemListener, ChangeListener {

    private final Object object;
    private final String field;

    public IncrementTristateCheckBox(String title, ApplicationContextProvider object, String field) {
        super(title);
        this.object = object;
        this.field = field;

        addChangeListener(this);
        addItemListener(this);

        object.getContext().addDependency(object, field, this);
    }

    public void itemStateChanged(ItemEvent e) {
        BaseUtils.invokeSetter(object, field, getStateAsBoolean());
    }

    public void stateChanged(ChangeEvent e) {
        BaseUtils.invokeSetter(object, field, getStateAsBoolean());
    }

    @Override
    protected void onChange() {
        super.onChange();
    }

    public void update(Object updateObject, String updateField) {
        setStateFromBoolean((Boolean) BaseUtils.invokeGetter(object, field));
    }
}
