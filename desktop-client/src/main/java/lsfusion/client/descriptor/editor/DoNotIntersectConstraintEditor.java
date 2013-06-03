package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.interop.form.layout.DoNotIntersectSimplexConstraint;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DoNotIntersectConstraintEditor extends JPanel {
    private DoNotIntersectSimplexConstraint constraint;

    private JCheckBox topBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.higher"));
    private JCheckBox leftBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.to.the.left"));
    private JCheckBox bottomBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.lower"));
    private JCheckBox rightBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.to.the.right"));
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

        topBox.addActionListener(actionListener);
        leftBox.addActionListener(actionListener);
        bottomBox.addActionListener(actionListener);
        rightBox.addActionListener(actionListener);

        recheck();
    }

    private void recheck(){
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

    public void setConstraint(DoNotIntersectSimplexConstraint constraint){
        this.constraint = constraint;
        recheck();
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
