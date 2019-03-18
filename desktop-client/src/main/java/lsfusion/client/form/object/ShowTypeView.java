package lsfusion.client.form.object;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.RmiQueue;
import lsfusion.client.form.layout.view.JComponentPanel;
import lsfusion.client.form.object.table.grid.user.toolbar.ToolbarGridButton;
import lsfusion.interop.form.property.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ShowTypeView extends JComponentPanel {
    private final ClassViewType[] types = new ClassViewType[] {ClassViewType.GRID, ClassViewType.PANEL};
    private final ImageIcon[] icons = new ImageIcon[] {new ImageIcon(ShowTypeView.class.getResource("/images/view_grid.png")),
                                                 new ImageIcon(ShowTypeView.class.getResource("/images/view_panel.png")),
                                                 new ImageIcon(ShowTypeView.class.getResource("/images/view_hide.png"))};
    private JButton[] buttons = new JButton[3];
    
    private final ShowTypeController controller;
    private final List<ClassViewType> banClassView;

    private ClassViewType classView = types[types.length - 1];

    public ShowTypeView(ShowTypeController controller, ClientShowType showType, List<ClassViewType> banClassView) {
        super(null);
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