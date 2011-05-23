package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.logics.ClientComponent;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.awt.*;

public class ComponentConstraintsEditor extends TitledPanel {

    protected SimplexConstraints<ClientComponent> constraints;
    protected TitledPanel fill;
    protected TitledPanel insetsSibling;
    protected TitledPanel directions;

    public ComponentConstraintsEditor(SimplexConstraints<ClientComponent> constraints) {
        super("Ограничения расположения");
        this.constraints = constraints;

        initialize();
    }

    private void initialize() {
        JTextField fillVertical = new IncrementTextEditor(constraints, "fillVertical");
        JTextField fillHorizontal = new IncrementTextEditor(constraints, "fillHorizontal");

        JTextField topDirection = new IncrementTextEditor(constraints, "directionsTop");
        JTextField leftDirection = new IncrementTextEditor(constraints, "directionsLeft");
        JTextField bottomDirection = new IncrementTextEditor(constraints, "directionsBottom");
        JTextField rightDirection = new IncrementTextEditor(constraints, "directionsRight");

        SingleInsetsEditor siblingEditor = new SingleInsetsEditor("insetsSibling");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));


        fill = new TitledPanel("Заполнение");
        fill.setLayout(new BoxLayout(fill, BoxLayout.X_AXIS));
        fill.add(new JLabel("по горизонтали: "));
        fill.add(fillHorizontal);
        fill.add(Box.createRigidArea(new Dimension(5, 5)));
        fill.add(new JLabel("по вертикали: "));
        fill.add(fillVertical);

        insetsSibling = new TitledPanel("Отступы относительно компонентов", siblingEditor);

        directions = new TitledPanel("Направление");
        directions.setLayout(new BoxLayout(directions, BoxLayout.X_AXIS));
        directions.add(new JLabel("вверх:"));
        directions.add(topDirection);
        directions.add(Box.createRigidArea(new Dimension(5, 5)));
        directions.add(new JLabel("влево:"));
        directions.add(leftDirection);
        directions.add(Box.createRigidArea(new Dimension(5, 5)));
        directions.add(new JLabel("вниз:"));
        directions.add(bottomDirection);
        directions.add(Box.createRigidArea(new Dimension(5, 5)));
        directions.add(new JLabel("вправо:"));
        directions.add(rightDirection);

        panel.add(fill);
        panel.add(insetsSibling);
        panel.add(directions);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    public boolean validateEditor() {
        return true;
    }

    public class SingleInsetsEditor extends JPanel {

        private JTextField topField;
        private JTextField leftField;
        private JTextField bottomField;
        private JTextField rightField;

        public SingleInsetsEditor(String field) {

            topField = new IncrementTextEditor(constraints, field + "Top");
            leftField = new IncrementTextEditor(constraints, field + "Left");
            bottomField = new IncrementTextEditor(constraints, field + "Bottom");
            rightField = new IncrementTextEditor(constraints, field + "Right");

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new JLabel("сверху:"));
            add(topField);
            add(Box.createRigidArea(new Dimension(5, 5)));
            add(new JLabel("слева:"));
            add(leftField);
            add(Box.createRigidArea(new Dimension(5, 5)));
            add(new JLabel("снизу:"));
            add(bottomField);
            add(Box.createRigidArea(new Dimension(5, 5)));
            add(new JLabel("справа:"));
            add(rightField);
        }
    }
}