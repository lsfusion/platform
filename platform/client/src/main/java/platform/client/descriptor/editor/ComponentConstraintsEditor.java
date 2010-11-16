package platform.client.descriptor.editor;

import platform.base.BaseUtils;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.logics.ClientComponent;
import platform.interop.context.IncrementView;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ComponentConstraintsEditor extends TitledPanel implements IncrementView {
    public ClientComponent component;
    public SimplexConstraints<ClientComponent> curConstraint;
    public String field;

    private JTextField fillVertical = new JTextField();
    private JTextField fillHorizontal = new JTextField();

    private JTextField topDirection = new JTextField();
    private JTextField leftDirection = new JTextField();
    private JTextField bottomDirection = new JTextField();
    private JTextField rightDirection = new JTextField();


    public ActionListener actionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            formObject();
            updateField();
        }
    };

    public ComponentConstraintsEditor(ClientComponent component, String field) {
        super("Ограничения расположения");

        this.component = component;
        this.field = field;

        curConstraint = this.component.constraints;

        initialize();
        setListeners();
        fill();
    }

    private void initialize() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        TitledPanel fill = new TitledPanel("Заполнение");
        fill.setLayout(new GridLayout(1, 4, 5, 5));
        fill.add(new JLabel("по горизонтали: "));
        fill.add(fillHorizontal);
        fill.add(new JLabel("по вертикали: "));
        fill.add(fillVertical);

        TitledPanel directions = new TitledPanel("Направление");
        directions.setLayout(new GridLayout(1, 8, 5, 5));
        directions.add(new JLabel("вверх:"));
        directions.add(topDirection);
        directions.add(new JLabel("влево:"));
        directions.add(leftDirection);
        directions.add(new JLabel("вниз:"));
        directions.add(bottomDirection);
        directions.add(new JLabel("вправо:"));
        directions.add(rightDirection);

        panel.add(fill);
        panel.add(directions);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    private void setListeners() {
        fillVertical.addActionListener(actionListener);
        fillHorizontal.addActionListener(actionListener);
        topDirection.addActionListener(actionListener);
        leftDirection.addActionListener(actionListener);
        bottomDirection.addActionListener(actionListener);
        rightDirection.addActionListener(actionListener);
    }

    private void fill() {
        if (curConstraint != null) {
            fillHorizontal.setText(String.valueOf(curConstraint.fillHorizontal));
            fillVertical.setText(String.valueOf(curConstraint.fillVertical));

            topDirection.setText(String.valueOf(curConstraint.directions.T));
            leftDirection.setText(String.valueOf(curConstraint.directions.L));
            bottomDirection.setText(String.valueOf(curConstraint.directions.B));
            rightDirection.setText(String.valueOf(curConstraint.directions.R));
        }
    }

    private void formObject() {
        if (!fillVertical.getText().equals("")) curConstraint.fillVertical = Double.parseDouble(fillVertical.getText());
        if (!fillHorizontal.getText().equals(""))
            curConstraint.fillHorizontal = Double.parseDouble(fillHorizontal.getText());

        if (!topDirection.getText().equals("")) curConstraint.directions.T = Double.parseDouble(topDirection.getText());
        if (!leftDirection.getText().equals(""))
            curConstraint.directions.L = Double.parseDouble(leftDirection.getText());
        if (!bottomDirection.getText().equals(""))
            curConstraint.directions.B = Double.parseDouble(bottomDirection.getText());
        if (!rightDirection.getText().equals(""))
            curConstraint.directions.R = Double.parseDouble(rightDirection.getText());
    }

    public void updateField() {
        BaseUtils.invokeSetter(component, field, curConstraint);
    }

    public void update(Object updateObject, String updateField) {
        curConstraint = (SimplexConstraints<ClientComponent>) BaseUtils.invokeGetter(component, field);
    }

    public boolean validateEditor() {
        return true;
    }
}