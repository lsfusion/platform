package platform.client.form.showtype;

import platform.client.form.ClientForm;
import platform.client.form.GroupObjectLogicsSupplier;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.io.IOException;

public abstract class ShowTypeView extends JPanel implements ActionListener {

    JRadioButton gridButton;
    JRadioButton panelButton;
    JRadioButton hideButton;

    GroupObjectLogicsSupplier logicsSupplier;

    public ShowTypeView(GroupObjectLogicsSupplier ilogicsSupplier) {

        logicsSupplier = ilogicsSupplier;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        gridButton = new JRadioButton("Таблица");
        gridButton.setActionCommand("grid");

        panelButton = new JRadioButton("Панель");
        panelButton.setActionCommand("panel");

        hideButton = new JRadioButton("Скрыть");
        hideButton.setActionCommand("hide");

        ButtonGroup showType = new ButtonGroup();
        showType.add(gridButton);
        showType.add(panelButton);
        showType.add(hideButton);

        for (Enumeration<AbstractButton> e = showType.getElements() ; e.hasMoreElements() ;) {

            AbstractButton button = e.nextElement();
            button.setMaximumSize(button.getMinimumSize());
            button.addActionListener(this);
            button.setFocusable(false);
            add(button);
        }
    }

    public void actionPerformed(ActionEvent e) {
        buttonPressed(e.getActionCommand());
    }

    public void changeClassView(Boolean classView, Boolean fixedClassView) {
        if (classView) {
            gridButton.setSelected(true);
            if (fixedClassView) {
                gridButton.setVisible(true);
                panelButton.setVisible(false);
            }
        }
        else {
            panelButton.setSelected(true);
            if (fixedClassView) {
                gridButton.setVisible(false);
                panelButton.setVisible(true);
            }
        }
    }

    protected abstract void buttonPressed(String action);
}