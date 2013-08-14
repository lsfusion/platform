package lsfusion.client.descriptor.editor;

import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementDoubleEditor;
import lsfusion.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import lsfusion.client.descriptor.increment.editor.IncrementTextEditor;
import lsfusion.client.logics.ClientComponent;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.FlexAlignment;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

import static lsfusion.client.ClientResourceBundle.getString;

public class ComponentConstraintsEditor extends TitledPanel {

    protected ClientComponent component;

    protected JPanel panel;

    protected TitledPanel flex;

    protected FlexAlignmentEditor alignment;

    public ComponentConstraintsEditor(ClientComponent component) {
        super(getString("descriptor.editor.location.limit"));
        this.component = component;

        initialize();
    }

    private void initialize() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        flex = new TitledPanel(getString("descriptor.editor.location.limit.flex"), new IncrementDoubleEditor(component, "flex"));
        alignment = new FlexAlignmentEditor(getString("descriptor.editor.location.limit.alignment"), "alignment");

        panel.add(flex);
        panel.add(alignment);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    public class AlignmentEditor extends TitledPanel {
        public AlignmentEditor(String description, String field) {
            super(description, new JComboBox(new IncrementSingleListSelectionModel(component, field) {
                @Override
                public java.util.List<?> getSingleList() {
                    return Arrays.asList(Alignment.values());
                }
            }));
        }
    }

    public class FlexAlignmentEditor extends TitledPanel {
        public FlexAlignmentEditor(String description, String field) {
            super(description, new JComboBox(new IncrementSingleListSelectionModel(component, field) {
                @Override
                public java.util.List<?> getSingleList() {
                    return Arrays.asList(FlexAlignment.values());
                }
            }));
        }
    }

    public class SingleInsetsEditor extends JPanel {
        private JTextField topField;
        private JTextField leftField;
        private JTextField bottomField;
        private JTextField rightField;

        public SingleInsetsEditor(String field) {

            topField = new IncrementTextEditor(component, field + "Top");
            leftField = new IncrementTextEditor(component, field + "Left");
            bottomField = new IncrementTextEditor(component, field + "Bottom");
            rightField = new IncrementTextEditor(component, field + "Right");

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new JLabel(getString("descriptor.editor.location.limit.directions.from.above")));
            add(topField);
            add(Box.createRigidArea(new Dimension(5, 5)));
            add(new JLabel(getString("descriptor.editor.location.limit.directions.on.the.right")));
            add(leftField);
            add(Box.createRigidArea(new Dimension(5, 5)));
            add(new JLabel(getString("descriptor.editor.location.limit.directions.from.below")));
            add(bottomField);
            add(Box.createRigidArea(new Dimension(5, 5)));
            add(new JLabel(getString("descriptor.editor.location.limit.directions.on.the.left")));
            add(rightField);
        }
    }
}