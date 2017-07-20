package lsfusion.client.form.showtype;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.RmiQueue;
import lsfusion.client.form.queries.ToolbarGridButton;
import lsfusion.client.logics.ClientShowType;
import lsfusion.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ShowTypeView extends JPanel {
    private final ClassViewType[] types = new ClassViewType[] {ClassViewType.GRID, ClassViewType.PANEL, ClassViewType.HIDE};
    private final ImageIcon[] icons = new ImageIcon[] {new ImageIcon(ShowTypeView.class.getResource("/images/view_grid.png")),
                                                 new ImageIcon(ShowTypeView.class.getResource("/images/view_panel.png")),
                                                 new ImageIcon(ShowTypeView.class.getResource("/images/view_hide.png"))};
    private JButton[] buttons = new JButton[3];
    
    private final ShowTypeController controller;
    private final List<ClassViewType> banClassView;

    private ClassViewType classView = types[types.length - 1];

    public ShowTypeView(ShowTypeController controller, ClientShowType showType, List<ClassViewType> banClassView) {
        this.controller = controller;
        this.banClassView = banClassView;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        for(int i=0;i<types.length;i++)
            add(buttons[i] = createShowTypeButton(types[i], icons[i]));
            
        setPreferredSize(new Dimension((ToolbarGridButton.DEFAULT_SIZE.width + 1) * 3, ToolbarGridButton.DEFAULT_SIZE.height));

        showType.installMargins(this);
    }

    private JButton createShowTypeButton(ClassViewType newClassView, ImageIcon icon) {
        JButton showTypeButton = new ToolbarGridButton(icon, ClientResourceBundle.getString("form.showtype." + newClassView.name().toLowerCase()));
        showTypeButton.addActionListener(new ShowTypeClassHandler(newClassView));
        return showTypeButton;
    }

    public void setClassView(ClassViewType iclassView) {
        classView = iclassView;

        for(int i=0;i<types.length;i++)
            buttons[i].setBorderPainted(classView != types[i]);

        for(int i=0;i<types.length;i++)
            buttons[i].setVisible(!banClassView.contains(types[i]));

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
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        controller.changeClassViewButtonClicked(newClassView);
                    }
                });
            }
        }
    }
}