package platform.client.form.sort;

import platform.client.Main;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MultiLineHeaderRenderer implements TableCellRenderer {

    protected final static ImageIcon arrowUpIcon = new ImageIcon(MultiLineHeaderRenderer.class.getResource("/images/arrowup.gif"));
    protected final static ImageIcon arrowDownIcon = new ImageIcon(MultiLineHeaderRenderer.class.getResource("/images/arrowdown.gif"));

    private final TableCellRenderer tableCellRenderer;
    private final SimplifiedRenderer simplifiedRenderer;

    public MultiLineHeaderRenderer(TableCellRenderer originalTableCellRenderer) {
        tableCellRenderer = originalTableCellRenderer;
        if (!Main.module.isFull()) {
            simplifiedRenderer = new SimplifiedRenderer(originalTableCellRenderer) {
                @Override
                protected Boolean getSortDirection(int column) {
                    return MultiLineHeaderRenderer.this.getSortDirection(column);
                }
            };
        } else {
            simplifiedRenderer = null;
        }
    }

    public Component getTableCellRendererComponent(JTable itable,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        if (Main.module.isFull()) {
            if (value instanceof String) {
                value = "<html>" + value + "</html>";
            }
        } else {
            return simplifiedRenderer.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
        }

        Component comp = tableCellRenderer.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setVerticalAlignment(JLabel.TOP);

            Boolean sortDir = getSortDirection(column);
            if (sortDir != null) {
                label.setIcon(sortDir ? arrowUpIcon : arrowDownIcon);
            } else {
                label.setIcon(null);
            }
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 10));
        }
        return comp;
    }

    protected Boolean getSortDirection(int column) {
        return null;
    }

    public static abstract class SimplifiedRenderer extends JPanel implements TableCellRenderer {
        private JTextArea textArea;
        private JLabel iconLabel;

        private final TableCellRenderer tableCellRenderer;

        public SimplifiedRenderer(TableCellRenderer originalTableCellRenderer) {
            tableCellRenderer = originalTableCellRenderer;

            setLayout(new BorderLayout());

            textArea = new JTextArea();
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setEditable(false);
            textArea.setHighlighter(null);

            iconLabel = new JLabel();

            setBorder(null);

            add(textArea, BorderLayout.CENTER);
            add(iconLabel, BorderLayout.WEST);
        }

        @Override
        public void setFont(Font font) {
            super.setFont(font);
            if (textArea != null) {
                textArea.setFont(font);
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component renderer = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            textArea.setForeground(renderer.getForeground());
            textArea.setBackground(renderer.getBackground());
            textArea.setFont(renderer.getFont().deriveFont(Font.PLAIN, 10));
            textArea.setText(value.toString());

            if (renderer instanceof JComponent) {
                this.setBorder(((JComponent) renderer).getBorder());
            }

            iconLabel.setHorizontalAlignment(JLabel.CENTER);
            iconLabel.setVerticalAlignment(JLabel.TOP);

            Boolean sortDir = getSortDirection(column);
            if (sortDir != null) {
                iconLabel.setIcon(sortDir ? arrowUpIcon : arrowDownIcon);
            } else {
                iconLabel.setIcon(null);
            }

            return this;
        }

        protected abstract Boolean getSortDirection(int column);
    }
}
