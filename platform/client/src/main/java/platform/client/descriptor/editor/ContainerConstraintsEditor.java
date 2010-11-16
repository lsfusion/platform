package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ContainerConstraintsEditor extends ComponentConstraintsEditor {

    private DoNotIntersectConstraintEditor doNotIntersectEditor;
    private SingleInsetsEditor siblingEditor;
    private SingleInsetsEditor insideEditor;
    private JTextField maxVars = new JTextField();

    private PropertyChangeListener propertyListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
            formObject();
            updateField();
        }
    };

    public ContainerConstraintsEditor(ClientComponent component, String field) {
        super(component, field);

        doNotIntersectEditor = new DoNotIntersectConstraintEditor(curConstraint.childConstraints);
        siblingEditor = new SingleInsetsEditor(curConstraint.insetsSibling);
        insideEditor = new SingleInsetsEditor(curConstraint.insetsInside);

        initialize();
    }

    private void initialize() {
        TitledPanel doNotIntersect = new TitledPanel("Пересечения", doNotIntersectEditor);

        TitledPanel insetsPanel = new TitledPanel("Отступы");
        insetsPanel.setLayout(new GridLayout(2, 1));
        TitledPanel insetsSibling = new TitledPanel("между потомками одних родителей", siblingEditor);
        TitledPanel insetsInside = new TitledPanel("внутри", insideEditor);
        insetsPanel.add(insetsSibling);
        insetsPanel.add(insetsInside);

        TitledPanel maxVarsPanel = new TitledPanel("");
        maxVarsPanel.setLayout(new GridLayout());
        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
        subPanel.add(new JLabel("Максимум переменных: "));
        subPanel.add(maxVars);
        maxVarsPanel.add(subPanel);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(doNotIntersect);
        panel.add(insetsPanel);
        panel.add(maxVarsPanel);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        doNotIntersectEditor.addPropertyChangeListener(propertyListener);
        siblingEditor.addPropertyChangeListener(propertyListener);
        insideEditor.addPropertyChangeListener(propertyListener);
        maxVars.addActionListener(actionListener);

        maxVars.setText(String.valueOf(curConstraint.maxVariables));
    }

    private void formObject() {
        if (!maxVars.getText().equals("")) curConstraint.maxVariables = Integer.parseInt(maxVars.getText());

        curConstraint.insetsSibling = siblingEditor.getObject();
        curConstraint.insetsInside = insideEditor.getObject();

        curConstraint.childConstraints = doNotIntersectEditor.getConstraint();
    }


    public class SingleInsetsEditor extends JPanel {
        private Insets object;

        private JTextField topField = new JTextField();
        private JTextField leftField = new JTextField();
        private JTextField bottomField = new JTextField();
        private JTextField rightField = new JTextField();

        private ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                firePropertyChange("Insets Modified", true, false);
            }
        };

        public SingleInsetsEditor(Insets object) {
            this.object = object;
            initialize();
            setListeners();
        }

        private void initialize() {
            setLayout(new GridLayout(1, 8, 10, 10));
            add(new JLabel("сверху:"));
            add(topField);
            add(new JLabel("слева:"));
            add(leftField);
            add(new JLabel("снизу:"));
            add(bottomField);
            add(new JLabel("справа:"));
            add(rightField);

            topField.setText(String.valueOf(object.top));
            leftField.setText(String.valueOf(object.left));
            bottomField.setText(String.valueOf(object.bottom));
            rightField.setText(String.valueOf(object.right));
        }

        public void setListeners() {
            topField.addActionListener(actionListener);
            leftField.addActionListener(actionListener);
            bottomField.addActionListener(actionListener);
            rightField.addActionListener(actionListener);
        }

        public Insets getObject() {
            object = new Insets(0, 0, 0, 0);
            if (!topField.getText().equals("")) object.top = Integer.parseInt(topField.getText());
            if (!leftField.getText().equals("")) object.left = Integer.parseInt(leftField.getText());
            if (!bottomField.getText().equals("")) object.bottom = Integer.parseInt(bottomField.getText());
            if (!rightField.getText().equals("")) object.right = Integer.parseInt(rightField.getText());
            return object;
        }
    }
}