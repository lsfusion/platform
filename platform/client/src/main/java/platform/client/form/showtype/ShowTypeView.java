package platform.client.form.showtype;

import platform.client.form.queries.ToolbarGridButton;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public abstract class ShowTypeView extends JPanel implements ActionListener {

    JButton gridButton;
    JButton panelButton;
    JButton hideButton;

    public ShowTypeView() {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        gridButton = new JButton("");
        gridButton.setIcon(new ImageIcon(ShowTypeView.class.getResource("/images/table.png")));
        gridButton.setToolTipText("Таблица");
        gridButton.setActionCommand("grid");

        panelButton = new JButton("");
        panelButton.setIcon(new ImageIcon(ShowTypeView.class.getResource("/images/list.png")));
        panelButton.setToolTipText("Панель");
        panelButton.setActionCommand("panel");

        hideButton = new JButton("");
        hideButton.setIcon(new ImageIcon(ShowTypeView.class.getResource("/images/close.png")));
        hideButton.setToolTipText("Скрыть");
        hideButton.setActionCommand("hide");

        add(gridButton);
        add(panelButton);
        add(hideButton);

        for (Component c : getComponents()) {
            AbstractButton button = (AbstractButton) c;
            button.setMinimumSize(ToolbarGridButton.BUTTON_SIZE);
            button.setMaximumSize(button.getMinimumSize());
            button.addActionListener(this);
            button.setFocusable(false);
        }
        setPreferredSize(new Dimension((ToolbarGridButton.BUTTON_SIZE.width + 1) * 3, ToolbarGridButton.BUTTON_SIZE.height));
    }

    public void actionPerformed(ActionEvent e) {
        buttonPressed(e.getActionCommand());
    }

    public void changeClassView(ClassViewType classView, List<ClassViewType> banClassView) {

        panelButton.setBorderPainted(classView != ClassViewType.PANEL);
        gridButton.setBorderPainted(classView != ClassViewType.GRID);
        hideButton.setBorderPainted(classView != ClassViewType.HIDE);

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