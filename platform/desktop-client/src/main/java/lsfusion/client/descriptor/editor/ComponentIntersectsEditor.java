package lsfusion.client.descriptor.editor;

import lsfusion.base.BaseUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.base.context.IncrementView;
import lsfusion.interop.form.layout.DoNotIntersectSimplexConstraint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ComponentIntersectsEditor extends TitledPanel implements IncrementView {
    String field;
    private ClientContainer container;
    private List<SingleIntersectEditor> editors = new ArrayList<SingleIntersectEditor>();

    private JPanel intersectsPanel = new JPanel();
    private JButton addBut = new JButton(ClientResourceBundle.getString("descriptor.editor.pending.add"));
    private JButton removeBut = new JButton(ClientResourceBundle.getString("descriptor.editor.pending.delete"));
    private PropertyChangeListener propListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
            updateField();
        }
    };

    public ComponentIntersectsEditor(String title, ClientContainer container, String field) {
        super(title);
        this.container = container;
        this.field = field;
        this.container.getContext().addDependency(this.container, "children", this);
        initialize();
    }

    private void initialize() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        addBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SingleIntersectEditor editor = new SingleIntersectEditor(container.children, null, null, null);
                editor.addPropertyChangeListener(propListener);
                editors.add(editor);
                refreshEditors();
            }
        });

        removeBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List<SingleIntersectEditor> list = new ArrayList<SingleIntersectEditor>(editors);
                for (SingleIntersectEditor editor : list) {
                    if (editor.isChecked()) {
                        editors.remove(editor);
                    }
                }
                refreshEditors();
            }
        });

        intersectsPanel.setLayout(new BoxLayout(intersectsPanel, BoxLayout.Y_AXIS));
        JPanel buts = new JPanel();
        buts.add(addBut);
        buts.add(removeBut);

        add(intersectsPanel);
        add(buts);
    }

    private void fillEditorsList(List<ClientComponent> componentList) {
        editors.clear();
        for (ClientComponent rightComponent : componentList) {
            for (ClientComponent leftComponent : rightComponent.constraints.intersects.keySet()) {
                SingleIntersectEditor editor = new SingleIntersectEditor(container.children, leftComponent, rightComponent.constraints.intersects.get(leftComponent), rightComponent);
                editor.addPropertyChangeListener(propListener);
                editors.add(editor);
            }
        }
    }

    private void refreshEditors() {
        intersectsPanel.removeAll();
        for (SingleIntersectEditor editor : editors) {
            intersectsPanel.add(editor);
        }
        revalidate();
    }

    private void updateField() {
        Map<ClientComponent, DoNotIntersectSimplexConstraint> currMap;
        for (ClientComponent component : container.children) {
            currMap = new HashMap<ClientComponent, DoNotIntersectSimplexConstraint>();
            for (SingleIntersectEditor editor : editors) {
                if (component.equals(editor.getRightComponent()) && editor.getConstraint().forbDir != 15) {
                    currMap.put(editor.getLeftComponent(), editor.getConstraint());
                }
            }
            BaseUtils.invokeSetter(component.constraints, field, currMap);
        }
    }

    public void update(Object updateObject, String updateField) {
        List<ClientComponent> childrenList = (List<ClientComponent>) BaseUtils.invokeGetter(updateObject, updateField);
        for (ClientComponent component : childrenList) {
            for (ClientComponent comp2 : component.constraints.intersects.keySet()) {
                if (!childrenList.contains(comp2)) {
                    component.constraints.intersects.remove(comp2);
                }
            }
        }
        fillEditorsList(childrenList);
        refreshEditors();
    }


    public class SingleIntersectEditor extends JPanel {
        private List<ClientComponent> children;
        private ClientComponent leftComponent, rightComponent;

        private JCheckBox check = new JCheckBox();
        private JComboBox leftBox = new JComboBox();
        private DoNotIntersectConstraintEditor editor;
        private JComboBox rightBox = new JComboBox();

        private ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                firePropertyChange("singleEditorChanged", true, false);
            }
        };

        public SingleIntersectEditor(List<ClientComponent> children, ClientComponent leftComponent, DoNotIntersectSimplexConstraint constraint, ClientComponent rightComponent) {
            this.children = children;
            this.leftComponent = leftComponent;
            this.rightComponent = rightComponent;

            if (constraint != null) {
                editor = new DoNotIntersectConstraintEditor(constraint);
            } else {
                editor = new DoNotIntersectConstraintEditor(new DoNotIntersectSimplexConstraint(15));
            }

            editor.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    firePropertyChange("singleEditorChanged", true, false);
                }
            });

            leftBox.addActionListener(actionListener);
            rightBox.addActionListener(actionListener);

            //чтобы комбобоксы не препятствовали изменению размеров панели
            leftBox.setMinimumSize(new Dimension(0, 0));
            rightBox.setMinimumSize(new Dimension(0, 0));

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            add(check);
            add(leftBox);
            add(editor);
            add(rightBox);

            fill();
        }

        private void fill() {
            for (ClientComponent component : children) {
                leftBox.addItem(component);
                rightBox.addItem(component);
            }
            if (leftComponent != null && rightComponent != null) {
                leftBox.setSelectedItem(leftComponent);
                rightBox.setSelectedItem(rightComponent);
            }
        }

        public boolean isChecked() {
            return check.isSelected();
        }

        public ClientComponent getLeftComponent() {
            return (ClientComponent) leftBox.getSelectedItem();
        }

        public ClientComponent getRightComponent() {
            return (ClientComponent) rightBox.getSelectedItem();
        }

        public DoNotIntersectSimplexConstraint getConstraint() {
            return editor.getConstraint();
        }
    }
}
