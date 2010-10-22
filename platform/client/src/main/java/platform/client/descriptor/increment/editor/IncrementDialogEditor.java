package platform.client.descriptor.increment.editor;

import platform.client.descriptor.editor.base.FlatButton;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.IncrementView;
import platform.base.BaseUtils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;

public abstract class IncrementDialogEditor extends FlatButton implements IncrementView {

    public void actionPerformed(ActionEvent e) {
        BaseUtils.invokeSetter(object, field, dialogValue(BaseUtils.invokeGetter(object, field)));
    }

    protected abstract Object dialogValue(Object currentValue);

    private final Object object;
    private final String field;

    public IncrementDialogEditor(Object object, String field) {
        this.object = object;
        this.field = field;

        IncrementDependency.add(object, field, this);
    }

    public void update(Object updateObject, String updateField) {
        setText(BaseUtils.invokeGetter(object, field).toString());
    }
}
