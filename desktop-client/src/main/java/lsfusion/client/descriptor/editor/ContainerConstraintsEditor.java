package lsfusion.client.descriptor.editor;

import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementIntegerEditor;
import lsfusion.client.logics.ClientContainer;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.ClientResourceBundle.getString;

public class ContainerConstraintsEditor extends ComponentConstraintsEditor {

    public ContainerConstraintsEditor(ClientContainer container) {
        super(container);
        initialize();
    }

    private void initialize() {
        JTextField gapX = new IncrementIntegerEditor(component, "gapX");
        JTextField gapY = new IncrementIntegerEditor(component, "gapY");

        JPanel gaps = new TitledPanel(getString("descriptor.editor.location.limit.gaps"));
        gaps.setLayout(new BoxLayout(gaps, BoxLayout.X_AXIS));
        gaps.add(new JLabel(getString("descriptor.editor.location.limit.gaps.x") + " "));
        gaps.add(gapX);
        gaps.add(Box.createRigidArea(new Dimension(5, 5)));
        gaps.add(new JLabel(getString("descriptor.editor.location.limit.gaps.y") + " "));
        gaps.add(gapY);

        JPanel columns = new TitledPanel(getString("descriptor.editor.location.limit.columns"), new IncrementIntegerEditor(component, "columns"));

        AlignmentEditor childrenAlignment = new AlignmentEditor(getString("descriptor.editor.location.limit.children.alignment"), "childrenAlignment");
        panel.add(childrenAlignment);
        panel.add(gaps);
        panel.add(columns);
    }
}