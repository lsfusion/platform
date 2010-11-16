package platform.client.descriptor.editor;

import platform.interop.form.layout.DoNotIntersectSimplexConstraint;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DoNotIntersectConstraintEditor extends JPanel {
    private DoNotIntersectSimplexConstraint constraint;

    private JCheckBox topBox = new JCheckBox("выше");
    private JCheckBox leftBox = new JCheckBox("левее");
    private JCheckBox bottomBox = new JCheckBox("ниже");
    private JCheckBox rightBox = new JCheckBox("правее");
    private ActionListener actionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            firePropertyChange("intersectChanged", true, false);
        }
    };

    public DoNotIntersectConstraintEditor(DoNotIntersectSimplexConstraint constraint) {
        this.constraint = constraint;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(topBox);
        add(leftBox);
        add(bottomBox);
        add(rightBox);
        initialize();
    }

    private void initialize() {
        topBox.addActionListener(actionListener);
        leftBox.addActionListener(actionListener);
        bottomBox.addActionListener(actionListener);
        rightBox.addActionListener(actionListener);

        if (constraint != null) {
            topBox.setSelected((constraint.forbDir & DoNotIntersectSimplexConstraint.TOP) == 0);
            leftBox.setSelected((constraint.forbDir & DoNotIntersectSimplexConstraint.LEFT) == 0);
            bottomBox.setSelected((constraint.forbDir & DoNotIntersectSimplexConstraint.BOTTOM) == 0);
            rightBox.setSelected((constraint.forbDir & DoNotIntersectSimplexConstraint.RIGHT) == 0);
        } else {
            topBox.setSelected(false);
            leftBox.setSelected(false);
            bottomBox.setSelected(false);
            rightBox.setSelected(false);
        }
    }

    public void setEnabled(boolean enable) {
        topBox.setEnabled(enable);
        leftBox.setEnabled(enable);
        bottomBox.setEnabled(enable);
        rightBox.setEnabled(enable);
    }

    public DoNotIntersectSimplexConstraint getConstraint() {
        constraint = new DoNotIntersectSimplexConstraint();
        if (!topBox.isSelected()) constraint.forbDir = constraint.forbDir | DoNotIntersectSimplexConstraint.TOP;
        if (!leftBox.isSelected()) constraint.forbDir = constraint.forbDir | DoNotIntersectSimplexConstraint.LEFT;
        if (!bottomBox.isSelected()) constraint.forbDir = constraint.forbDir | DoNotIntersectSimplexConstraint.BOTTOM;
        if (!rightBox.isSelected()) constraint.forbDir = constraint.forbDir | DoNotIntersectSimplexConstraint.RIGHT;
        return constraint;
    }
}
