package platform.client.form.classes;

import platform.client.FlatRolloverButton;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class ClassContainer extends JPanel {

    private JScrollPane pane;

    private JButton expandButton;
    private JPanel buttonContainer;

    public ClassContainer(ClassTree view) {
        Dimension BUTTON_SIZE = new Dimension(20, 20);
        setLayout(new BorderLayout());

        pane = new JScrollPane(view);
        add(pane, BorderLayout.CENTER);

        JButton collapseButton = new FlatRolloverButton(new ImageIcon(ComponentDesign.class.getResource("/images/side_hide.png")));
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

        expandButton = new FlatRolloverButton(new ImageIcon(ComponentDesign.class.getResource("/images/side_expand.png")));
        expandButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                expandTree();
            }
        });

        Insets insets = expandButton.getInsets();
        insets.left = 0; insets.right = 0;
        expandButton.setMargin(insets); // экономим на спичках
        expandButton.setFocusable(false);
        expandButton.setFont(getFont().deriveFont(Font.BOLD));
        expandButton.setVisible(false); // по умолчанию невидима

        add(expandButton, BorderLayout.EAST);

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

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.X_AXIS));
        buttonContainer.add(collapseButton);
        buttonContainer.add(Box.createHorizontalGlue());
        buttonContainer.add(widthDecButton);
        buttonContainer.add(widthIncButton);

        add(buttonContainer, BorderLayout.SOUTH);

        // по умолчанию показываем дерево свернутым
        collapseTree();
    }

    private Dimension maxSize;

    private void expandTree() {

        expandButton.setVisible(false);

        pane.setVisible(true);
        buttonContainer.setVisible(true);

        setMaximumSize(maxSize);

        needToBeRevalidated();
    }

    protected void collapseTree() {

        expandButton.setVisible(true);
        pane.setVisible(false);
        buttonContainer.setVisible(false);

        maxSize = getMaximumSize();

        Dimension newMaxSize = new Dimension(maxSize);
        newMaxSize.width = expandButton.getMinimumSize().width;
        setMaximumSize(newMaxSize);

        needToBeRevalidated();
    }

    protected abstract void needToBeRevalidated();
    protected abstract void widthDecreased();
    protected abstract void widthIncreased();
}
