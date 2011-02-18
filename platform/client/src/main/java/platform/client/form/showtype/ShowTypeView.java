package platform.client.form.showtype;

import platform.client.form.GroupObjectLogicsSupplier;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.List;

public abstract class ShowTypeView extends JPanel implements ActionListener {

    JButton gridButton;
    JButton panelButton;
    JButton hideButton;

    GroupObjectLogicsSupplier logicsSupplier;
    final private Dimension buttonSize = new Dimension(18, 18);

    public ShowTypeView(GroupObjectLogicsSupplier ilogicsSupplier) {

        logicsSupplier = ilogicsSupplier;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        gridButton = new JButton("");
        gridButton.setIcon(new ImageIcon(ShowTypeView.class.getResource("/platform/images/table.png")));
        gridButton.setActionCommand("grid");

        panelButton = new JButton("");
        panelButton.setIcon(new ImageIcon(ShowTypeView.class.getResource("/platform/images/list.png")));
        panelButton.setActionCommand("panel");

        hideButton = new JButton("");
        hideButton.setIcon(new ImageIcon(ShowTypeView.class.getResource("/platform/images/close.png")));
        hideButton.setActionCommand("hide");

        add(gridButton);
        add(panelButton);
        add(hideButton);

        for (Component c : getComponents()) {
            AbstractButton button = (AbstractButton) c;
            button.setMinimumSize(buttonSize);
            button.setMaximumSize(button.getMinimumSize());
            button.addActionListener(this);
            button.setFocusable(false);
        }
        setPreferredSize(new Dimension((buttonSize.width + 1) * 3, buttonSize.height));
    }

    public void actionPerformed(ActionEvent e) {
        buttonPressed(e.getActionCommand());
    }

    public void changeClassView(ClassViewType classView, List<ClassViewType> banClassView) {

        panelButton.setBorderPainted(classView == ClassViewType.PANEL ? false : true);
        gridButton.setBorderPainted(classView == ClassViewType.GRID ? false : true);
        hideButton.setBorderPainted(classView == ClassViewType.HIDE ? false : true);

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