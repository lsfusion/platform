package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementTextEditor;
import lsfusion.client.logics.ClientComponent;
import lsfusion.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.awt.*;

public class ContainerConstraintsEditor extends ComponentConstraintsEditor {

    public ContainerConstraintsEditor(SimplexConstraints<ClientComponent> constraints) {
        super(constraints);
        initialize();
    }

    private void initialize() {
        ChildConstraintsEditor childConstraintsEditor = new ChildConstraintsEditor(constraints, "childConstraints");

        SingleInsetsEditor insideEditor = new SingleInsetsEditor("insetsInside");

        JTextField maxVars = new IncrementTextEditor(constraints, "maxVariables");

        TitledPanel doNotIntersect = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.intersections"), childConstraintsEditor);

        TitledPanel insetsInside = new TitledPanel(ClientResourceBundle.getString("descriptor.editor.margins.of.borders"), insideEditor);

        TitledPanel maxVarsPanel = new TitledPanel("");
        maxVarsPanel.setLayout(new BoxLayout(maxVarsPanel, BoxLayout.X_AXIS));
        maxVarsPanel.add(new JLabel((ClientResourceBundle.getString("descriptor.editor.variables.maximum")+": ")));
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