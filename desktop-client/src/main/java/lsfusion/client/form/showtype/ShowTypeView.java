package lsfusion.client.form.showtype;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.queries.ToolbarGridButton;
import lsfusion.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ShowTypeView extends JPanel {
    private final static ImageIcon gridIcon = new ImageIcon(ShowTypeView.class.getResource("/images/view_grid.png"));
    private final static ImageIcon panelIcon = new ImageIcon(ShowTypeView.class.getResource("/images/view_panel.png"));
    private final static ImageIcon hideIcon = new ImageIcon(ShowTypeView.class.getResource("/images/view_hide.png"));

    private final JButton gridButton;
    private final JButton panelButton;
    private final JButton hideButton;
    private final ShowTypeController controller;
    private final List<ClassViewType> banClassView;

    private ClassViewType classView = ClassViewType.HIDE;

    public ShowTypeView(ShowTypeController controller, List<ClassViewType> banClassView) {
        this.controller = controller;
        this.banClassView = banClassView;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        add(gridButton = createShowTypeButton(ClassViewType.GRID, gridIcon));
        add(panelButton = createShowTypeButton(ClassViewType.PANEL, panelIcon));
        add(hideButton = createShowTypeButton(ClassViewType.HIDE, hideIcon));

        setPreferredSize(new Dimension((ToolbarGridButton.DEFAULT_SIZE.width + 1) * 3, ToolbarGridButton.DEFAULT_SIZE.height));
    }

    private JButton createShowTypeButton(ClassViewType newClassView, ImageIcon icon) {
        JButton showTypeButton = new ToolbarGridButton(icon, ClientResourceBundle.getString("form.showtype." + newClassView.name().toLowerCase()));
        showTypeButton.addActionListener(new ShowTypeClassHandler(newClassView));
        return showTypeButton;
    }

    public void setClassView(ClassViewType iclassView) {
        classView = iclassView;

        panelButton.setBorderPainted(classView != ClassViewType.PANEL);
        gridButton.setBorderPainted(classView != ClassViewType.GRID);
        hideButton.setBorderPainted(classView != ClassViewType.HIDE);

        panelButton.setVisible(!banClassView.contains(ClassViewType.PANEL));
        gridButton.setVisible(!banClassView.contains(ClassViewType.GRID));
        hideButton.setVisible(!banClassView.contains(ClassViewType.HIDE));

        setVisible(banClassView.size() < 2);
    }

    public class ShowTypeClassHandler implements ActionListener {
        private final ClassViewType newClassView;

        public ShowTypeClassHandler(ClassViewType newClassView) {
            this.newClassView = newClassView;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (classView != newClassView) {
                controller.changeClassViewButtonClicked(newClassView);
            }
        }
    }
}