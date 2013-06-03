package lsfusion.client.descriptor.editor;

import lsfusion.client.descriptor.editor.base.AbstractTristateCheckBox;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

class CheckRenderer extends JPanel implements TreeCellRenderer {
    protected AbstractTristateCheckBox check;

    protected lsfusion.client.descriptor.editor.CheckRenderer.TreeLabel label;

    public CheckRenderer() {
        setLayout(null);
        add(check = new AbstractTristateCheckBox() {
            protected void onChange() {
                // ничего не делаем
            }
        });
        add(label = new lsfusion.client.descriptor.editor.CheckRenderer.TreeLabel());
        check.setBackground(UIManager.getColor("Tree.textBackground"));
        label.setForeground(UIManager.getColor("Tree.textForeground"));
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean isSelected, boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, isSelected,
                expanded, leaf, row, hasFocus);
        setEnabled(tree.isEnabled());
        check.setState(((GroupPropertyObjectEditor.CheckNode) value).selectedState);
        label.setFont(tree.getFont());
        label.setText(stringValue);
        label.isSelected = isSelected;
        label.hasFocus = hasFocus;
        if (leaf) {
            label.setIcon(UIManager.getIcon("Tree.leafIcon"));
        } else if (expanded) {
            label.setIcon(UIManager.getIcon("Tree.openIcon"));
        } else {
            label.setIcon(UIManager.getIcon("Tree.closedIcon"));
        }
        return this;
    }

    public Dimension getPreferredSize() {
        Dimension d_check = check.getPreferredSize();
        Dimension d_label = label.getPreferredSize();
        return new Dimension(d_check.width + d_label.width,
                (d_check.height < d_label.height ? d_label.height
                        : d_check.height));
    }

    public void doLayout() {
        Dimension d_check = check.getPreferredSize();
        Dimension d_label = label.getPreferredSize();
        int y_check = 0;
        int y_label = 0;
        if (d_check.height < d_label.height) {
            y_check = (d_label.height - d_check.height) / 2;
        } else {
            y_label = (d_check.height - d_label.height) / 2;
        }
        // почему-то надо -2 иначе налазит на рамки
        check.setBounds(0, y_check, d_check.width, d_check.height-2);
        label.setBounds(d_check.width, y_label, d_label.width, d_label.height-2);
    }

    public void setBackground(Color color) {
        if (color instanceof ColorUIResource)
            color = null;
        super.setBackground(color);
    }

    public class TreeLabel extends JLabel {
        boolean isSelected;

        boolean hasFocus;

        public TreeLabel() {
        }

        public void setBackground(Color color) {
            if (color instanceof ColorUIResource)
                color = null;
            super.setBackground(color);
        }

        public void paint(Graphics g) {
            String str;
            if ((str = getText()) != null) {
                if (0 < str.length()) {
                    if (isSelected) {
                        g.setColor(UIManager.getColor("Tree.selectionBackground"));
                    } else {
                        g.setColor(UIManager.getColor("Tree.textBackground"));
                    }
                    Dimension d = getPreferredSize();
                    int imageOffset = 0;
                    Icon currentI = getIcon();
                    if (currentI != null) {
                        imageOffset = currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
                    }
                    g.fillRect(imageOffset, 0, d.width - 1 - imageOffset, d.height);
                    if (hasFocus) {
                        g.setColor(UIManager.getColor("Tree.selectionBorderColor"));
                        //g.drawRect(imageOffset, 0, d.width - 1 - imageOffset, d.height - 1);
                    }
                }
            }
            super.paint(g);
        }

        public Dimension getPreferredSize() {
            Dimension retDimension = super.getPreferredSize();
            if (retDimension != null) {
                retDimension = new Dimension(retDimension.width + 3,
                        retDimension.height);
            }
            return retDimension;
        }
    }
}
