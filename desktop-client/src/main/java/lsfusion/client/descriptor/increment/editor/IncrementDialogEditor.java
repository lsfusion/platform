package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.ReflectionUtils;
import lsfusion.client.descriptor.editor.base.FlatButton;
import lsfusion.base.context.*;

public abstract class IncrementDialogEditor extends FlatButton implements IncrementView {

    protected void onClick() {
        Object dialogResult = dialogValue(ReflectionUtils.invokeGetter(object, field));
        if(dialogResult!=null)
            ReflectionUtils.invokeSetter(object, field, dialogResult);
    }

    protected abstract Object dialogValue(Object currentValue);

    private final Object object;
    private final String field;

    public IncrementDialogEditor(ApplicationContextProvider object, String field) {
        this.object = object;
        this.field = field;

        object.getContext().addDependency(object, field, this);
    }

    public void update(Object updateObject, String updateField) {
        Object value = ReflectionUtils.invokeGetter(object, field);
        setText(value==null?"":value.toString());
    }
}
