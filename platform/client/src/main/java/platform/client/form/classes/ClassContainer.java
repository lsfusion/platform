package platform.client.form.classes;

import platform.client.FlatRolloverButton;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class ClassContainer extends JPanel {

    private JButton collapseButton;
    public boolean isExpanded;

    public ClassContainer(ClassTree view) {
        Dimension BUTTON_SIZE = new Dimension(20, 20);
        setLayout(new BorderLayout());

        JScrollPane pane = new JScrollPane(view);
        add(pane, BorderLayout.CENTER);

        collapseButton = new FlatRolloverButton(new ImageIcon(ComponentDesign.class.getResource("/images/side_hide.gif")));
        collapseButton.setMinimumSize(BUTTON_SIZE);
        collapseButton.setPreferredSize(BUTTON_SIZE);
        collapseButton.setMaximumSize(BUTTON_SIZE);
        collapseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                collapseTree();
            }
        });
        collapseButton.setFocusable(false);
        collapseButton.setFont(getFont().deriveFont(Font.BOLD));

        JButton widthDecButton = new FlatRolloverButton(new ImageIcon(ComponentDesign.class.getResource("/images/expand_dec.png")));
        widthDecButton.setMinimumSize(BUTTON_SIZE);
        widthDecButton.setPreferredSize(BUTTON_SIZE);
        widthDecButton.setMaximumSize(BUTTON_SIZE);
        widthDecButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                widthDecreased();
                needToBeRevalidated();
            }
        });
        widthDecButton.setFocusable(false);
        widthDecButton.setFont(getFont().deriveFont(Font.BOLD));

        JButton widthIncButton = new FlatRolloverButton(new ImageIcon(ComponentDesign.class.getResource("/images/expand_inc.png")));
        widthIncButton.setMinimumSize(BUTTON_SIZE);
        widthIncButton.setPreferredSize(BUTTON_SIZE);
        widthIncButton.setMaximumSize(BUTTON_SIZE);
        widthIncButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                widthIncreased();
                needToBeRevalidated();
            }
        });
        widthIncButton.setFocusable(false);
        widthIncButton.setFont(getFont().deriveFont(Font.BOLD));

        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.X_AXIS));
        buttonContainer.add(collapseButton);
        buttonContainer.add(Box.createHorizontalGlue());
        buttonContainer.add(widthDecButton);
        buttonContainer.add(widthIncButton);

        add(buttonContainer, BorderLayout.SOUTH);

        // по умолчанию прячем дерево
        collapseTree();
    }

    public void expandTree() {
        isExpanded = true;
        setVisible(true);
        needToBeRevalidated();
    }

    protected void collapseTree() {
        isExpanded = false;
        setVisible(false);
        needToBeRevalidated();
    }
    
    public JButton getCollapseButton() {
        return collapseButton;
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag && isExpanded);
    }

    protected abstract void needToBeRevalidated();
    protected abstract void widthDecreased();
    protected abstract void widthIncreased();
}
