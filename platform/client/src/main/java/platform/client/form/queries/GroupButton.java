package platform.client.form.queries;

import platform.base.OrderedMap;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.form.grid.GridTable;
import platform.client.form.grid.GridTableModel;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientIntegralClass;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

public abstract class GroupButton extends ToolbarGridButton {

    public GroupDialog dialog;

    public GroupButton() {
        super("/images/group.png", "Группировка");
    }

    public abstract void addListener();

    public class GroupDialog extends JDialog {
        GridTable table;
        GridTableModel tableModel;
        Map<Integer, List<byte[]>> selectedGroupMap = new OrderedMap<Integer, List<byte[]>>();
        Map<Integer, List<byte[]>> selectedSumMap = new OrderedMap<Integer, List<byte[]>>();
        Map<Integer, List<byte[]>> selectedMaxMap = new OrderedMap<Integer, List<byte[]>>();
        List<JCheckBox> groupChecks = new ArrayList<JCheckBox>();
        List<JCheckBox> sumChecks = new ArrayList<JCheckBox>();
        List<JCheckBox> maxChecks = new ArrayList<JCheckBox>();
        JCheckBox notNullCheck = new JCheckBox("Только заполненные");
        JLabel recordCountLabel;
        JScrollPane scroll;
        final int RECORD_QUANTITY_ID = -1;

        public GroupDialog(Frame owner, GridTable table) throws IOException {
            super(owner, "Группировка", true);
            this.table = table;
            this.tableModel = table.getTableModel();

            setMinimumSize(new Dimension(670, 300));
            Rectangle bounds = owner.getBounds();
            bounds.x += 20;
            bounds.y += 20;
            bounds.width -= 40;
            bounds.height -= 40;
            setBounds(bounds);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());

            ActionListener escListener = new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    setVisible(false);
                }
            };
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

            TitledPanel groupByPanel = new TitledPanel("Группировать по");
            JScrollPane groupScrollPane = new JScrollPane();
            groupScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            JPanel insideGPanel = new JPanel();
            insideGPanel.setLayout(new BoxLayout(insideGPanel, BoxLayout.Y_AXIS));
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                JCheckBox checkBox = new JCheckBox(tableModel.getColumnName(i).trim());
                if (i == table.getSelectedColumn()) {
                    checkBox.setSelected(true);
                }
                groupChecks.add(checkBox);
                insideGPanel.add(checkBox);
            }
            groupScrollPane.setViewportView(insideGPanel);
            groupScrollPane.setBorder(new EmptyBorder(0,0,0,0));
            groupByPanel.add(groupScrollPane);

            JPanel groupPanel = new JPanel();
            groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
            JCheckBox box = new JCheckBox("Кол-во записей");
            box.setSelected(true);
            sumChecks.add(box);
            groupPanel.add(box);

            TitledPanel sumPanel = new TitledPanel("Суммировать");
            sumPanel.setLayout(new BoxLayout(sumPanel, BoxLayout.Y_AXIS));
            JScrollPane sumScrollPane = new JScrollPane();
            sumScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            JPanel insideSPanel = new JPanel();
            insideSPanel.setLayout(new BoxLayout(insideSPanel, BoxLayout.Y_AXIS));
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                ClientPropertyDraw property = tableModel.getColumnProperty(i);
                JCheckBox checkBox = new JCheckBox(tableModel.getColumnName(i).trim());
                sumChecks.add(checkBox);
                if (property.baseType instanceof ClientIntegralClass) {
                    insideSPanel.add(checkBox);
                }
            }
            sumScrollPane.setViewportView(insideSPanel);
            sumScrollPane.setBorder(new EmptyBorder(0,0,0,0));
            sumPanel.add(sumScrollPane);

            groupPanel.add(sumPanel);

            TitledPanel maxPanel = new TitledPanel("Максимум");
            maxPanel.setLayout(new BoxLayout(maxPanel, BoxLayout.Y_AXIS));
            JScrollPane maxScrollPane = new JScrollPane();
            maxScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            JPanel insideMPanel = new JPanel();
            insideMPanel.setLayout(new BoxLayout(insideMPanel, BoxLayout.Y_AXIS));
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                ClientPropertyDraw property = tableModel.getColumnProperty(i);
                JCheckBox checkBox = new JCheckBox(tableModel.getColumnName(i).trim());
                maxChecks.add(checkBox);
                if (property.baseType instanceof ClientIntegralClass) {
                    insideMPanel.add(checkBox);
                }
            }
            maxScrollPane.setViewportView(insideMPanel);
            maxScrollPane.setBorder(new EmptyBorder(0,0,0,0));
            maxPanel.add(maxScrollPane);

            groupPanel.add(maxPanel);

            JPanel checkNButtonPanel = new JPanel();
            checkNButtonPanel.setLayout(new BorderLayout());
            JButton execute = new JButton("Результат");
            execute.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    buttonPressed();
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                }
            });
            recordCountLabel = new JLabel();
            JPanel labelPanel = new JPanel();
            labelPanel.add(recordCountLabel);
            checkNButtonPanel.add(notNullCheck, BorderLayout.WEST);
            checkNButtonPanel.add(labelPanel, BorderLayout.CENTER);
            checkNButtonPanel.add(execute, BorderLayout.EAST);

            JPanel westPanel = new JPanel();
            westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.X_AXIS));
            westPanel.add(groupByPanel);
            westPanel.add(groupPanel);
            westPanel.add(Box.createRigidArea(new Dimension(5, 5)));
            add(westPanel, BorderLayout.WEST);

            scroll = new JScrollPane();
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BorderLayout());
            centerPanel.add(scroll, BorderLayout.CENTER);
            centerPanel.add(checkNButtonPanel, BorderLayout.SOUTH);

            add(centerPanel, BorderLayout.CENTER);

            List<byte[]> list = new ArrayList<byte[]>();
            int columnIndex = table.getSelectedColumn();
            if (columnIndex == -1) {
                columnIndex = 0;
                groupChecks.get(0).setSelected(true);
            }
            ClientPropertyDraw defaultProperty = tableModel.getColumnProperty(columnIndex);
            list.add(tableModel.getColumnKey(columnIndex).serialize(defaultProperty));
            selectedGroupMap.put(defaultProperty.getID(), list);
            selectedSumMap.put(RECORD_QUANTITY_ID, null);
        }

        private void buttonPressed() {
            selectedGroupMap = new OrderedMap<Integer, List<byte[]>>();
            for (int i = 0; i < groupChecks.size(); i++) {
                if (groupChecks.get(i).isSelected()) {
                    ClientPropertyDraw property = tableModel.getColumnProperty(i);
                    List<byte[]> list = selectedGroupMap.get(property.getID());
                    if (list == null) {
                        list = new ArrayList<byte[]>();
                    }
                    try {
                        list.add(tableModel.getColumnKey(i).serialize(property));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    selectedGroupMap.put(property.getID(), list);
                }
            }

            selectedSumMap = new OrderedMap<Integer, List<byte[]>>();
            if (sumChecks.get(0).isSelected()) {
                selectedSumMap.put(RECORD_QUANTITY_ID, null);
            }
            for (int i = 0; i < sumChecks.size() - 1; i++) {
                if (sumChecks.get(i + 1).isSelected()) {
                    ClientPropertyDraw property = tableModel.getColumnProperty(i);
                    List<byte[]> list = selectedSumMap.get(property.getID());
                    if (list == null) {
                        list = new ArrayList<byte[]>();
                    }
                    try {
                        list.add(tableModel.getColumnKey(i).serialize(property));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    selectedSumMap.put(property.getID(), list);
                }
            }

            selectedMaxMap = new OrderedMap<Integer, List<byte[]>>();
            for (int i = 0; i < maxChecks.size(); i++) {
                if (maxChecks.get(i).isSelected()) {
                    ClientPropertyDraw property = tableModel.getColumnProperty(i);
                    List<byte[]> list = selectedMaxMap.get(property.getID());
                    if (list == null) {
                        list = new ArrayList<byte[]>();
                    }
                    try {
                        list.add(tableModel.getColumnKey(i).serialize(property));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    selectedMaxMap.put(property.getID(), list);
                }
            }
        }

        public Map<Integer, List<byte[]>> getSelectedGroupMap() {
            return selectedGroupMap;
        }

        public Map<Integer, List<byte[]>> getSelectedSumMap() {
            return selectedSumMap;
        }

        public Map<Integer, List<byte[]>> getSelectedMaxMap() {
            return selectedMaxMap;
        }

        public boolean onlyNotNull() {
            return notNullCheck.isSelected();
        }

        public void update(Map<List<Object>, List<Object>> values) {
            List<Integer> minSizes = new ArrayList<Integer>();
            List<Integer> maxSizes = new ArrayList<Integer>();
            List<Integer> prefSizes = new ArrayList<Integer>();
            List<String> names = new ArrayList<String>();
            for (JCheckBox box : groupChecks) {
                if (box.isSelected()) {
                    names.add(tableModel.getColumnName(groupChecks.indexOf(box)).trim());
                    minSizes.add(tableModel.getColumnProperty(groupChecks.indexOf(box)).getMinimumWidth(table));
                    maxSizes.add(tableModel.getColumnProperty(groupChecks.indexOf(box)).getMaximumWidth(table));
                    prefSizes.add(tableModel.getColumnProperty(groupChecks.indexOf(box)).getPreferredWidth(table));
                }
            }
            if (sumChecks.get(0).isSelected()) {
                names.add("Записей");
                minSizes.add(56);
                maxSizes.add(533900);
                prefSizes.add(56);
            }
            for (int i = 1; i < sumChecks.size(); i++) {
                if (sumChecks.get(i).isSelected()) {
                    names.add(tableModel.getColumnName(i - 1).trim() + " [S]");
                    minSizes.add(tableModel.getColumnProperty(i - 1).getMinimumWidth(table));
                    maxSizes.add(tableModel.getColumnProperty(i - 1).getMaximumWidth(table));
                    prefSizes.add(tableModel.getColumnProperty(i - 1).getPreferredWidth(table));
                }
            }

            for (int i = 0; i < maxChecks.size(); i++) {
                if (maxChecks.get(i).isSelected()) {
                    names.add(tableModel.getColumnName(i).trim() + " [M]");
                    minSizes.add(tableModel.getColumnProperty(i).getMinimumWidth(table));
                    maxSizes.add(tableModel.getColumnProperty(i).getMaximumWidth(table));
                    prefSizes.add(tableModel.getColumnProperty(i).getPreferredWidth(table));
                }
            }

            CustomModel model = new CustomModel(names, values);
            JTable table = new JTable(model) {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    if (autoResizeMode != AUTO_RESIZE_OFF) {
                        if (getParent() instanceof JViewport) {
                            return (getParent().getWidth() > getPreferredSize().width);
                        }
                    }
                    return false;
                }

                @Override
                public Dimension getPreferredSize() {
                    if (getParent() instanceof JViewport) {
                        if (getParent().getWidth() < super.getPreferredSize().width) {
                            return getMinimumSize();
                        }
                    }
                    return super.getPreferredSize();
                }
            };
            table.setCellSelectionEnabled(true);
            table.setRowSorter(new TableRowSorter(model));

            for (int i = 0; i < model.getColumnCount(); ++i) {
                TableColumn column = table.getColumnModel().getColumn(i);
                JTableHeader header = table.getTableHeader();
                header.setFont(header.getFont().deriveFont(header.getFont().getStyle(), 10));
                column.setPreferredWidth(prefSizes.get(i));
                column.setMinWidth(minSizes.get(i));
                column.setMaxWidth(maxSizes.get(i));
            }
            scroll.setViewportView(table);
            recordCountLabel.setText("Всего записей: " + table.getRowCount());
        }

        public class CustomModel extends AbstractTableModel {
            protected Map<List<Object>, List<Object>> values;
            protected List<String> columnNames;

            public CustomModel(List<String> names, Map<List<Object>, List<Object>> values) {
                this.values = values;
                columnNames = names;
            }

            public Class getColumnClass(int column) {
                Object value = null;
                for (int i = 0; i < getRowCount(); i++) {
                    value = getValueAt(i, column);
                    if (value != null) {
                        break;
                    }
                }
                return value != null ? value.getClass() : String.class;
            }

            public int getColumnCount() {
                return columnNames.size();
            }

            public int getRowCount() {
                return values.size();
            }

            public String getColumnName(int col) {
                return "<html>" + columnNames.get(col) + "</html>";
            }

            public Object getValueAt(int row, int column) {
                Object[] keySet = values.keySet().toArray();
                int keysQuantity = ((List<Object>) keySet[0]).size();
                if (column <= keysQuantity - 1) {
                    Object value = (((List<Object>) keySet[row]).get(column));
                    return value.getClass().equals(String.class) ? value.toString().trim() : value;
                } else {
                    return ((List<Object>) values.values().toArray()[row]).get(column - keysQuantity);
                }
            }
        }
    }
}
