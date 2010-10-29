package platform.client.form.showtype;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.List;

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

    public void changeClassView(ClassViewType classView, List<ClassViewType> banClassView) {

        if(classView == ClassViewType.PANEL) panelButton.setSelected(true);
        if(classView == ClassViewType.GRID) gridButton.setSelected(true);
        if(classView == ClassViewType.HIDE) hideButton.setSelected(true);

        int visibleCount = 0;

        panelButton.setVisible(!banClassView.contains(ClassViewType.PANEL));
        gridButton.setVisible(!banClassView.contains(ClassViewType.GRID));
        hideButton.setVisible(!banClassView.contains(ClassViewType.HIDE));

        if (panelButton.isVisible()) visibleCount++;
        if (gridButton.isVisible()) visibleCount++;
        if (hideButton.isVisible()) visibleCount++;

        setVisible(visibleCount > 1);
    }

    protected abstract void buttonPressed(String action);
}