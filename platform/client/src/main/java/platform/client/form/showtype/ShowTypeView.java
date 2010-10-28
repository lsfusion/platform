package platform.client.form.showtype;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.interop.ClassViewType;
import platform.interop.ClassViewTypeEnum;

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

    public void changeClassView(Byte classView, List<ClassViewTypeEnum> banClassView) {

        switch (classView) {
            case ClassViewType.PANEL : panelButton.setSelected(true); break;
            case ClassViewType.GRID : gridButton.setSelected(true); break;
            case ClassViewType.HIDE : hideButton.setSelected(true); break;
        }

        int visibleCount = 0;

        panelButton.setVisible(!banClassView.contains(ClassViewTypeEnum.valueOf("Panel")));
        gridButton.setVisible(!banClassView.contains(ClassViewTypeEnum.valueOf("Grid")));
        hideButton.setVisible(!banClassView.contains(ClassViewTypeEnum.valueOf("Hide")));

        if (panelButton.isVisible()) visibleCount++;
        if (gridButton.isVisible()) visibleCount++;
        if (hideButton.isVisible()) visibleCount++;

        setVisible(visibleCount > 1);
    }

    protected abstract void buttonPressed(String action);
}