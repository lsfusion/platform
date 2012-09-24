package platform.client.form.showtype;

import platform.client.ClientResourceBundle;
import platform.client.form.queries.ToolbarGridButton;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public abstract class ShowTypeView extends JPanel {
    private final static ImageIcon gridIcon = new ImageIcon(ShowTypeView.class.getResource("/images/table.png"));
    private final static ImageIcon panelIcon = new ImageIcon(ShowTypeView.class.getResource("/images/list.png"));
    private final static ImageIcon hideIcon = new ImageIcon(ShowTypeView.class.getResource("/images/close.png"));

    private final JButton gridButton;
    private final JButton panelButton;
    private final JButton hideButton;

    public ShowTypeView() {
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

    public void changeClassView(ClassViewType classView, List<ClassViewType> banClassView) {
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
            setNewClassView(newClassView);
        }
    }

    protected abstract void setNewClassView(ClassViewType newClassView);
}