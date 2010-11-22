package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.logics.ClientComponent;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.awt.*;

public class ContainerConstraintsEditor extends ComponentConstraintsEditor {

    private DoNotIntersectConstraintEditor doNotIntersectEditor;
    private SingleInsetsEditor insideEditor;
    private JTextField maxVars;

    public ContainerConstraintsEditor(SimplexConstraints<ClientComponent> constraints) {
        super(constraints);
        initialize();
    }

    private void initialize() {
        doNotIntersectEditor = new DoNotIntersectConstraintEditor(constraints.childConstraints);
        insideEditor = new SingleInsetsEditor("insetsInside");

        maxVars = new IncrementTextEditor(constraints, "maxVariables");

        TitledPanel doNotIntersect = new TitledPanel("Пересечения", doNotIntersectEditor);

        TitledPanel insetsInside = new TitledPanel("Отступы от границ", insideEditor);

        TitledPanel maxVarsPanel = new TitledPanel("");
        maxVarsPanel.setLayout(new BoxLayout(maxVarsPanel, BoxLayout.X_AXIS));
        maxVarsPanel.add(new JLabel("Максимум переменных: "));
        maxVarsPanel.add(maxVars);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(doNotIntersect);
        panel.add(fill);
        panel.add(insetsInside);
        panel.add(insetsSibling);
        panel.add(directions);
        panel.add(maxVarsPanel);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }
}