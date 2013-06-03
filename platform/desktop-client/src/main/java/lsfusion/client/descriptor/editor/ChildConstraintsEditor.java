package lsfusion.client.descriptor.editor;

import lsfusion.base.BaseUtils;
import lsfusion.base.context.IncrementView;
import lsfusion.client.logics.ClientComponent;
import lsfusion.interop.form.layout.DoNotIntersectSimplexConstraint;
import lsfusion.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ChildConstraintsEditor extends JPanel implements IncrementView {

    private String field;
    private SimplexConstraints<ClientComponent> constraints;
    private DoNotIntersectConstraintEditor editor;

    public ChildConstraintsEditor(SimplexConstraints<ClientComponent> constraints, String field){
        this.constraints = constraints;
        this.field = field;
        editor = new DoNotIntersectConstraintEditor(this.constraints.childConstraints);

        editor.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                updateField();
            }
        });

        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(editor);

        this.constraints.getContext().addDependency(this.constraints, "childConstraints", this);
    }

    private void updateField() {
        BaseUtils.invokeSetter(constraints, field, editor.getConstraint());
    }

    public void update(Object updateObject, String updateField) {
        DoNotIntersectSimplexConstraint value = (DoNotIntersectSimplexConstraint) BaseUtils.invokeGetter(updateObject, updateField);
        editor.setConstraint(value);
    }
}
