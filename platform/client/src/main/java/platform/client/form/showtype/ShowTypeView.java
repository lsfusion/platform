package platform.client.form.showtype;

import platform.client.ClientResourceBundle;
import platform.client.FlatRolloverButton;
import platform.client.form.queries.ToolbarGridButton;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public abstract class ShowTypeView extends JPanel {

    FlatRolloverButton gridButton;
    FlatRolloverButton panelButton;
    FlatRolloverButton hideButton;

    public ShowTypeView() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        add(gridButton = createShowTypeButton(ClassViewType.GRID, "table.png"));
        add(panelButton = createShowTypeButton(ClassViewType.PANEL, "list.png"));
        add(hideButton = createShowTypeButton(ClassViewType.HIDE, "close.png"));

        setPreferredSize(new Dimension((ToolbarGridButton.BUTTON_SIZE.width + 1) * 3, ToolbarGridButton.BUTTON_SIZE.height));
    }

    private FlatRolloverButton createShowTypeButton(ClassViewType newClassView, String icon) {
        FlatRolloverButton showTypeButton = new FlatRolloverButton("");

        showTypeButton.setFocusable(false);
        showTypeButton.setMinimumSize(ToolbarGridButton.BUTTON_SIZE);
        showTypeButton.setMaximumSize(ToolbarGridButton.BUTTON_SIZE);
        showTypeButton.setIcon(new ImageIcon(ShowTypeView.class.getResource("/images/" + icon)));
        showTypeButton.setToolTipText(ClientResourceBundle.getString("form.showtype." + newClassView.name().toLowerCase()));
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