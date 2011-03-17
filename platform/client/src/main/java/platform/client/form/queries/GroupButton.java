package platform.client.form.queries;

import platform.base.OrderedMap;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.form.grid.GridTableModel;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientIntegralClass;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public abstract class GroupButton extends JButton {

    public GroupDialog dialog;

    public GroupButton() {
        super();
        Icon icon = new ImageIcon(getClass().getResource("/images/group.png"));
        setIcon(icon);
        setAlignmentY(Component.TOP_ALIGNMENT);
        Dimension buttonSize = new Dimension(20, 20);
        setMinimumSize(buttonSize);
        setPreferredSize(buttonSize);
        setMaximumSize(buttonSize);
        setFocusable(false);
        setToolTipText("Группировка");
        addListener();
    }

    public abstract void addListener();

    public class GroupDialog extends JDialog {
        GridTableModel tableModel;
        Map<Integer, List<byte[]>> selectedGroupMap = new OrderedMap<Integer, List<byte[]>>();
        Map<Integer, List<byte[]>> selectedSumMap = new OrderedMap<Integer, List<byte[]>>();
        List<JCheckBox> groupChecks = new ArrayList<JCheckBox>();
        List<JCheckBox> sumChecks = new ArrayList<JCheckBox>();
        JScrollPane scroll;
        final int RECORD_QUANTITY_ID = -1;

        public GroupDialog(Frame owner, int defaultGroupPropertyColumn, GridTableModel tableModel) throws IOException {
            super(owner, "Группировка", true);
            this.tableModel = tableModel;

            setMinimumSize(new Dimension(250, 350));
            setSize(600, 450);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());

            ActionListener escListener = new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    setVisible(false);
                }
            };
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

            TitledPanel groupPanel = new TitledPanel("Группировать по");
            groupPanel.setLayout(new GridLayout(0, 4));

            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                JCheckBox checkBox = new JCheckBox(tableModel.getColumnName(i).trim());
                if (i == defaultGroupPropertyColumn) {
                    checkBox.setSelected(true);
                }
                groupChecks.add(checkBox);
                groupPanel.add(checkBox);
            }

            TitledPanel sumPanel = new TitledPanel("Суммировать");
            sumPanel.setLayout(new GridLayout(0, 4));

            JCheckBox box = new JCheckBox("Кол-во записей");
            box.setSelected(true);
            sumChecks.add(box);
            sumPanel.add(box);
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                ClientPropertyDraw property = tableModel.getColumnProperty(i);
                    JCheckBox checkBox = new JCheckBox(tableModel.getColumnName(i).trim());
                    sumChecks.add(checkBox);
                if (property.baseType instanceof ClientIntegralClass) {
                    sumPanel.add(checkBox);
                }
            }

            JButton execute = new JButton("Результат");
            execute.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    buttonPressed();
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                }
            });
            execute.setAlignmentX(JComponent.CENTER_ALIGNMENT);

            JPanel northPanel = new JPanel();
            northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
            northPanel.add(groupPanel);
            northPanel.add(sumPanel);
            northPanel.add(Box.createRigidArea(new Dimension(5, 5)));
            northPanel.add(execute);
            northPanel.add(Box.createRigidArea(new Dimension(5, 5)));
            add(northPanel, BorderLayout.NORTH);

            scroll = new JScrollPane();
            add(scroll, BorderLayout.CENTER);

            List<byte[]> list = new ArrayList<byte[]>();
            ClientPropertyDraw defaultProperty = tableModel.getColumnProperty(defaultGroupPropertyColumn);
            list.add(tableModel.getColumnKey(defaultGroupPropertyColumn).serialize(defaultProperty));
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
        }

        public Map<Integer, List<byte[]>> getSelectedGroupMap() {
            return selectedGroupMap;
        }

        public Map<Integer, List<byte[]>> getSelectedSumMap() {
            return selectedSumMap;
        }

        public void update(Map<List<Object>, List<Object>> values) {
            List<String> names = new ArrayList<String>();
            for (JCheckBox box : groupChecks) {
                if (box.isSelected()) {
                    names.add(tableModel.getColumnName(groupChecks.indexOf(box)).trim());
                }
            }
            if (sumChecks.get(0).isSelected()) {
                names.add("Кол-во записей");
            }
            for (int i = 1; i < sumChecks.size(); i++){
                if (sumChecks.get(i).isSelected()) {
                    names.add(tableModel.getColumnName(i - 1).trim());
                }
            }

            JTable table = new JTable(new CustomModel(names, values));
            table.setCellSelectionEnabled(true);
            scroll.setViewportView(table);
        }
    }

    public class CustomModel extends AbstractTableModel {
        protected Map<List<Object>, List<Object>> values;
        protected List<String> columnNames;

        public CustomModel(List<String> names, Map<List<Object>, List<Object>> values) {
            this.values = values;
            columnNames = names;
        }

        public int getColumnCount() {
            return columnNames.size();
        }

        public int getRowCount() {
            return values.size();
        }

        public String getColumnName(int col) {
            return columnNames.get(col);
        }

        public Object getValueAt(int row, int column) {
            Object[] keySet = values.keySet().toArray();
            int keysQuantity = ((List<Object>) keySet[0]).size();
            if (column <= keysQuantity - 1) {
                return (((List<Object>) keySet[row]).get(column)).toString().trim();
            } else {
                return format(((List<Object>) values.values().toArray()[row]).get(column - keysQuantity));
            }
        }

        public String format(Object number) {
            NumberFormat nf = NumberFormat.getNumberInstance();
            return nf.format(number);
        }
    }
}
