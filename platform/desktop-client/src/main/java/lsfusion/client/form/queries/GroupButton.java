package lsfusion.client.form.queries;

import jxl.CellView;
import jxl.Workbook;
import jxl.write.*;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.*;
import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.form.grid.GridTable;
import lsfusion.client.form.grid.GridTableModel;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientIntegralClass;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Number;
import java.util.*;
import java.util.List;

public abstract class GroupButton extends ToolbarGridButton {

    private static final ImageIcon groupIcon = new ImageIcon(FilterView.class.getResource("/images/group.png"));

    public GroupDialog dialog;

    public GroupButton() {
        super(groupIcon, ClientResourceBundle.getString("form.queries.grouping"));
    }

    public abstract void addListener();

    public class GroupDialog extends JDialog {
        private GridTable initialTable;
        private GridTableModel initialTableModel;
        private List<List<Integer>> selectedGroupLevels = new ArrayList<List<Integer>>();
        private Map<Integer, List<byte[]>> selectedSumMap = new OrderedMap<Integer, List<byte[]>>();
        private Map<Integer, List<byte[]>> selectedMaxMap = new OrderedMap<Integer, List<byte[]>>();
        private List<JCheckBox> groupChecks = new ArrayList<JCheckBox>();
        private List<JSpinner> groupSpinners = new ArrayList<JSpinner>();
        private List<JCheckBox> sumChecks = new ArrayList<JCheckBox>();
        private List<JCheckBox> maxChecks = new ArrayList<JCheckBox>();
        private JCheckBox notNullCheck = new JCheckBox(ClientResourceBundle.getString("form.queries.only.filled"));
        private JLabel recordCountLabel = new JLabel();
        private JScrollPane treeTableScroll = new JScrollPane();
        private JXTreeTable treeTable;
        private CustomTreeTableModel treeTableModel;
        private JPanel expandButtonsPanel = new JPanel();
        private final int RECORD_QUANTITY_ID = -1;

        private ChangeListener spinnerListener = new ChangeListener() {
            public void stateChanged(ChangeEvent ce) {
                verifySpinners();
            }
        };

        public GroupDialog(Frame owner, GridTable initialTable) throws IOException {
            super(owner, ClientResourceBundle.getString("form.queries.grouping"), true);
            this.initialTable = initialTable;
            initialTableModel = initialTable.getTableModel();

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

            TitledPanel groupByPanel = new TitledPanel(ClientResourceBundle.getString("form.queries.groupby"));
            JPanel allFieldsPanel = new JPanel();
            allFieldsPanel.setLayout(new BoxLayout(allFieldsPanel, BoxLayout.Y_AXIS));

            for (int i = 0; i < initialTableModel.getColumnCount(); i++) {
                final JCheckBox checkBox = new JCheckBox(initialTableModel.getColumnName(i).trim());
                if (i == initialTable.getSelectedColumn()) {
                    checkBox.setSelected(true);
                }
                checkBox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        final JSpinner spinner = groupSpinners.get(groupChecks.indexOf(checkBox));
                        spinner.setVisible(checkBox.isSelected());
                        if (checkBox.isSelected()) {
                            spinner.setValue(getMaxSpinnerValue() + 1);
                            //ставим фокус на спиннер
                            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().requestFocusInWindow();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().selectAll();
                                }
                            });
                        }
                        revalidate();
                        verifySpinners();
                    }
                });
                groupChecks.add(checkBox);

                JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
                spinner.setMinimumSize(new Dimension(35, 19));
                spinner.setPreferredSize(new Dimension(35, 19));
                spinner.setVisible(checkBox.isSelected());
                spinner.addChangeListener(spinnerListener);
                groupSpinners.add(spinner);

                JPanel fieldPanel = new JPanel();
                fieldPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                fieldPanel.add(checkBox);
                fieldPanel.add(spinner);
                fieldPanel.setPreferredSize(new Dimension(fieldPanel.getPreferredSize().width + spinner.getPreferredSize().width, checkBox.getPreferredSize().height + 3));
                allFieldsPanel.add(fieldPanel);
            }
            groupByPanel.setLayout(new BorderLayout());
            groupByPanel.add(allFieldsPanel, BorderLayout.NORTH);

            JScrollPane groupScrollPane = new JScrollPane();
            groupScrollPane.setViewportView(groupByPanel);

            JPanel quantityAndGroupPanel = new JPanel();
            quantityAndGroupPanel.setLayout(new BoxLayout(quantityAndGroupPanel, BoxLayout.Y_AXIS));
            JCheckBox box = new JCheckBox(ClientResourceBundle.getString("form.queries.number.of.entries"));
            box.setSelected(true);
            sumChecks.add(box);
            quantityAndGroupPanel.add(box);

            TitledPanel sumPanel = new TitledPanel(ClientResourceBundle.getString("form.queries.sum"));
            sumPanel.setLayout(new BoxLayout(sumPanel, BoxLayout.Y_AXIS));
            JScrollPane sumScrollPane = new JScrollPane();

            TitledPanel maxPanel = new TitledPanel(ClientResourceBundle.getString("form.queries.maximum"));
            maxPanel.setLayout(new BoxLayout(maxPanel, BoxLayout.Y_AXIS));
            JScrollPane maxScrollPane = new JScrollPane();

            for (int i = 0; i < initialTableModel.getColumnCount(); i++) {
                ClientPropertyDraw property = initialTableModel.getColumnProperty(i);
                String propertyCaption = initialTableModel.getColumnName(i).trim();
                JCheckBox checkBoxSum = new JCheckBox(propertyCaption);
                sumChecks.add(checkBoxSum);
                JCheckBox checkBoxMax = new JCheckBox(propertyCaption);
                maxChecks.add(checkBoxMax);
                if (property.baseType instanceof ClientIntegralClass) {
                    sumPanel.add(checkBoxSum);
                    maxPanel.add(checkBoxMax);
                }
            }

            sumScrollPane.setViewportView(sumPanel);
            maxScrollPane.setViewportView(maxPanel);

            JSplitPane groupPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sumScrollPane, maxScrollPane);
            groupPanel.setContinuousLayout(true);
            groupPanel.setOneTouchExpandable(true);
            quantityAndGroupPanel.add(groupPanel);

            JButton resetAllButton = new JButton(ClientResourceBundle.getString("form.queries.reset"));
            resetAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < groupChecks.size(); i++) {
                        groupChecks.get(i).setSelected(false);
                        groupSpinners.get(i).setVisible(false);
                    }
                    for (JCheckBox box : sumChecks)
                        box.setSelected(false);
                    for (JCheckBox box : maxChecks)
                        box.setSelected(false);
                }
            });

            JButton executeButton = new JButton(ClientResourceBundle.getString("form.queries.result"));
            executeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        buttonPressed();
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                }
            });

            JPanel resetPanel = new JPanel();
            resetPanel.add(resetAllButton);

            JPanel labelPanel = new JPanel();
            labelPanel.add(recordCountLabel);

            JPanel checkNButtonPanel = new JPanel();
            checkNButtonPanel.add(notNullCheck);
            checkNButtonPanel.add(executeButton);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.add(labelPanel, BorderLayout.CENTER);
            bottomPanel.add(checkNButtonPanel, BorderLayout.EAST);
            bottomPanel.add(resetPanel, BorderLayout.WEST);

            JSplitPane westPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, groupScrollPane, quantityAndGroupPanel);
            westPanel.setContinuousLayout(true);
            westPanel.setOneTouchExpandable(true);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(expandButtonsPanel, BorderLayout.WEST);
            JButton excelExport = new JButton(ClientResourceBundle.getString("form.queries.export.to.excel"));
            excelExport.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        exportToExcel();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            topPanel.add(excelExport, BorderLayout.EAST);

            JPanel eastPanel = new JPanel();
            eastPanel.setLayout(new BorderLayout());
            eastPanel.add(topPanel, BorderLayout.NORTH);
            eastPanel.add(treeTableScroll, BorderLayout.CENTER);

            JSplitPane allPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westPanel, eastPanel);
            allPanel.setContinuousLayout(true);
            allPanel.setOneTouchExpandable(true);
            add(bottomPanel, BorderLayout.SOUTH);
            add(allPanel, BorderLayout.CENTER);

            int columnIndex = initialTable.getSelectedColumn();
            if (columnIndex == -1) {     //для пустых гридов. указываем выбранным по умолчанию первое свойство
                columnIndex = 0;
                groupChecks.get(0).setSelected(true);
                groupSpinners.get(0).setVisible(true);
            }
            selectedGroupLevels.add(BaseUtils.toList(columnIndex));
            selectedSumMap.put(RECORD_QUANTITY_ID, null);
        }

        private void verifySpinners() {
            int maxValue = getMaxSpinnerValue();
            for (int i = 1; i <= maxValue; i++) {
                boolean decrease = true;
                for (JSpinner spinner : groupSpinners) {
                    if (spinner.isVisible() && (Integer) spinner.getValue() == i) {
                        decrease = false;
                        break;
                    }
                }
                if (decrease) {
                    for (JSpinner spinner : groupSpinners) {
                        if (spinner.isVisible() && (Integer) spinner.getValue() > i) {
                            spinner.removeChangeListener(spinnerListener);
                            spinner.setValue(spinner.getPreviousValue());
                            spinner.addChangeListener(spinnerListener);
                        }
                    }
                    maxValue--;
                    i--;
                }
            }
        }

        private int getMaxSpinnerValue() {
            int maxValue = 1;
            for (JSpinner spinner : groupSpinners) {
                if (spinner.isVisible() && (Integer) spinner.getValue() > maxValue) {
                    maxValue = (Integer) spinner.getValue();
                }
            }
            return maxValue;
        }

        private void buttonPressed() throws IOException {
            selectedGroupLevels = new ArrayList<List<Integer>>();
            List<Integer> level = new ArrayList<Integer>();
            for (int k = 1; k <= getMaxSpinnerValue(); k++) {
                List<Integer> newLevel = new ArrayList<Integer>(level);
                for (int i = 0; i < groupChecks.size(); i++) {
                    if (groupSpinners.get(i).isVisible() && (Integer) groupSpinners.get(i).getValue() == k) {
                        newLevel.add(i);
                    }
                }
                level = new ArrayList<Integer>(newLevel);
                selectedGroupLevels.add(newLevel);
            }

            selectedSumMap = new OrderedMap<Integer, List<byte[]>>();
            if (sumChecks.get(0).isSelected()) {
                selectedSumMap.put(RECORD_QUANTITY_ID, null);
            }
            for (int i = 0; i < sumChecks.size() - 1; i++) {
                if (sumChecks.get(i + 1).isSelected()) {
                    ClientPropertyDraw property = initialTableModel.getColumnProperty(i);
                    List<byte[]> list = selectedSumMap.get(property.getID());
                    if (list == null) {
                        list = new ArrayList<byte[]>();
                    }
                    list.add(initialTableModel.getColumnKey(i).serialize());
                    selectedSumMap.put(property.getID(), list);
                }
            }

            selectedMaxMap = new OrderedMap<Integer, List<byte[]>>();
            for (int i = 0; i < maxChecks.size(); i++) {
                if (maxChecks.get(i).isSelected()) {
                    ClientPropertyDraw property = initialTableModel.getColumnProperty(i);
                    List<byte[]> list = selectedMaxMap.get(property.getID());
                    if (list == null) {
                        list = new ArrayList<byte[]>();
                    }
                    list.add(initialTableModel.getColumnKey(i).serialize());
                    selectedMaxMap.put(property.getID(), list);
                }
            }
        }

        public List<Map<Integer, List<byte[]>>> getSelectedGroupLevels() throws IOException {
            List<Map<Integer, List<byte[]>>> selectedGroupProperties = new ArrayList<Map<Integer, List<byte[]>>>();
            for (List<Integer> level : selectedGroupLevels) {
                Map<Integer, List<byte[]>> groupLevel = new OrderedMap<Integer, List<byte[]>>();
                for (Integer index : level) {
                    ClientPropertyDraw property = initialTableModel.getColumnProperty(index);
                    List<byte[]> list = groupLevel.get(property.getID());
                    if (list == null) {
                        list = new ArrayList<byte[]>();
                    }
                    list.add(initialTableModel.getColumnKey(index).serialize());
                    groupLevel.put(property.getID(), list);
                }
                selectedGroupProperties.add(groupLevel);
            }
            return selectedGroupProperties;
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

        public void update(List<Map<List<Object>, List<Object>>> values) {
            List<Integer> minSizes = new ArrayList<Integer>();
            List<Integer> maxSizes = new ArrayList<Integer>();
            List<Integer> prefSizes = new ArrayList<Integer>();
            List<String> names = new ArrayList<String>();
            for (int i = 1; i <= getMaxSpinnerValue(); i++) {
                for (JSpinner spinner : groupSpinners) {
                    if (spinner.isVisible() && (Integer) spinner.getValue() == i) {
                        int spinnerIndex = groupSpinners.indexOf(spinner);
                        ClientPropertyDraw property = initialTableModel.getColumnProperty(spinnerIndex);
                        names.add(initialTableModel.getColumnName(spinnerIndex).trim());
                        minSizes.add(property.getMinimumWidth(initialTable));
                        maxSizes.add(property.getMaximumWidth(initialTable));
                        prefSizes.add(property.getPreferredWidth(initialTable));
                    }
                }
            }
            if (sumChecks.get(0).isSelected()) {
                names.add(ClientResourceBundle.getString("form.queries.nodes"));
                minSizes.add(56);
                maxSizes.add(533900);
                prefSizes.add(56);
            }
            for (int i = 1; i < sumChecks.size(); i++) {
                if (sumChecks.get(i).isSelected()) {
                    ClientPropertyDraw property = initialTableModel.getColumnProperty(i - 1);
                    names.add(initialTableModel.getColumnName(i - 1).trim() + " [S]");
                    minSizes.add(property.getMinimumWidth(initialTable));
                    maxSizes.add(property.getMaximumWidth(initialTable));
                    prefSizes.add(property.getPreferredWidth(initialTable));
                }
            }

            for (int i = 0; i < maxChecks.size(); i++) {
                if (maxChecks.get(i).isSelected()) {
                    ClientPropertyDraw property = initialTableModel.getColumnProperty(i);
                    names.add(initialTableModel.getColumnName(i).trim() + " [M]");
                    minSizes.add(property.getMinimumWidth(initialTable));
                    maxSizes.add(property.getMaximumWidth(initialTable));
                    prefSizes.add(property.getPreferredWidth(initialTable));
                }
            }

            treeTableModel = new CustomTreeTableModel(selectedGroupLevels.get(selectedGroupLevels.size() - 1).size(), names, values);
            treeTable = new JXTreeTable(treeTableModel) {   //перегружаем методы для обхода бага с горизонтальным скроллбаром у JTable'а

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
            treeTable.setCellSelectionEnabled(true);
            treeTable.setShowGrid(true, true);
            treeTable.setTableHeader(new JTableHeader(treeTable.getColumnModel()) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    int index = columnModel.getColumnIndexAtX(e.getPoint().x);
                    return treeTableModel.getColumnName(index);
                }
            });
            treeTable.getTableHeader().setReorderingAllowed(false);
            int additionalWidth = 19 * (values.size() - 1);
            treeTable.getColumnModel().getColumn(0).setMinWidth(55 + additionalWidth);  //ширина колонки с деревом
            JTableHeader header = treeTable.getTableHeader();
            header.addMouseListener(treeTableModel.new ColumnListener());  //для сортировки
            header.setFont(header.getFont().deriveFont(header.getFont().getStyle(), 10));
            for (int i = 0; i < treeTableModel.getColumnCount() - 1; i++) {
                TableColumn column = treeTable.getColumnModel().getColumn(i + 1);
                column.setPreferredWidth(prefSizes.get(i));
                column.setMinWidth(minSizes.get(i));
                column.setMaxWidth(maxSizes.get(i));
            }
            treeTableScroll.setViewportView(treeTable);

            createExpandButtons();
            recordCountLabel.setText(ClientResourceBundle.getString("form.queries.total.nodes")+" " + treeTableModel.getLastLevelRowCount());
        }

        private void createExpandButtons() {
            expandButtonsPanel.removeAll();
            expandButtonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            final List<JButton> buttons = new ArrayList<JButton>();
            for (int i = 0; i <= treeTableModel.getLevelCount() - 1; i++) {
                JButton button = new JButton("" + i);
                if (i == 0) {
                    button.setText("-");
                    button.setToolTipText(ClientResourceBundle.getString("form.queries.collapse.all"));
                } else {
                    button.setToolTipText(ClientResourceBundle.getString("form.queries.expand.level")+" " + i);
                }
                buttons.add(button);
                button.setMargin(new Insets(0,0,0,0));
                button.setBorder(new LineBorder(Color.BLACK));
                button.setBackground(Color.WHITE);
                button.setContentAreaFilled(false);
                button.setOpaque(true);
                button.setFocusable(false);
                Dimension buttonSize = new Dimension(14, 14);
                button.setFont(button.getFont().deriveFont(button.getFont().getStyle(), 8));
                button.setMaximumSize(buttonSize);
                button.setPreferredSize(buttonSize);
                button.addActionListener(new ActionListener() {
                    Integer value;

                    public void actionPerformed(ActionEvent e) {
                        JButton source = (JButton) e.getSource();
                        value = buttons.indexOf(source);
                        expand(null, treeTableModel.getRoot());
                    }

                    private void expand(Object[] parents, TreeTableNode node) {
                        if (parents == null || (Integer) node.getUserObject() <= value) {
                            TreePath pathToExpand;
                            Object[] newParents;
                            if (parents != null) {
                                pathToExpand = new TreePath(BaseUtils.add(parents, node));
                                newParents = BaseUtils.add(parents, node);
                            } else {
                                pathToExpand = new TreePath(node);
                                newParents = new Object[]{node};
                            }
                            treeTable.expandPath(pathToExpand);
                            for (int i = 0; i < node.getChildCount(); i++) {
                                expand(newParents, node.getChildAt(i));
                            }
                        } else {
                            treeTable.collapsePath(new TreePath(BaseUtils.add(parents, node)));
                        }
                    }
                });
                expandButtonsPanel.add(button);
            }
            expandButtonsPanel.revalidate();
            expandButtonsPanel.repaint();
            if (expandButtonsPanel.getComponentCount() != 0) {
                ((JButton) expandButtonsPanel.getComponents()[expandButtonsPanel.getComponentCount() - 1]).doClick(); //разворачиваем всё дерево
            }
        }

        public class CustomTreeTableModel extends DefaultTreeTableModel {
            List<String> columnNames;
            List<Map<List<Object>, List<Object>>> sources;
            Map<DefaultMutableTreeTableNode, List<Object>> values = new OrderedMap<DefaultMutableTreeTableNode, List<Object>>();
            int keyColumnsQuantity = 0;

            public CustomTreeTableModel(int keyColumnsQuantity, List<String> columnNames, List<Map<List<Object>, List<Object>>> values) {
                super();
                this.columnNames = columnNames;
                sources = values;

                DefaultMutableTreeTableNode rootNode = new DefaultMutableTreeTableNode("Root", true);
                this.keyColumnsQuantity = keyColumnsQuantity;
                if (!values.isEmpty()) {
                    addNodes(rootNode, 0, null);
                }
                root = rootNode;
            }

            public int getLevelCount() {
                int count = 0;
                for (Map<List<Object>, List<Object>> level : sources) {
                    if (level.isEmpty()) {
                        break;
                    }
                    count++;
                }
                return count;
            }

            public int getLastLevelRowCount() {
                int rowCount = 0;
                for (int i = sources.size() - 1; i >= 0; i--) {
                    if (!sources.get(i).isEmpty()) {
                        rowCount += sources.get(i).size();
                        break;
                    }
                }
                return rowCount;
            }

            private boolean containsAll(List<Object> parent, List<Object> child) {
                for (int i = 0; i < parent.size(); i++) {
                    if (!parent.get(i).equals(child.get(i))) {
                        return false;
                    }
                }
                return true;
            }

            private void addNodes(DefaultMutableTreeTableNode parentNode, int index, List<Object> parentKeys) {
                Map<List<Object>, List<Object>> map = sources.get(index);
                for (List<Object> keys : map.keySet()) {
                    if (parentKeys == null || containsAll(parentKeys, keys)) {
                        List<Object> row = new ArrayList<Object>();
                        row.addAll(keys);
                        for (int i = 0; i < keyColumnsQuantity - keys.size(); i++) {
                            row.add(null);
                        }
                        row.addAll(map.get(keys));
                        DefaultMutableTreeTableNode node = new DefaultMutableTreeTableNode(index + 1, true);
                        parentNode.add(node);
                        values.put(node, row);
                        if (index < sources.size() - 1) {
                            addNodes(node, index + 1, keys);
                        }
                    }
                }
            }

            @Override
            public Class getColumnClass(int column) {
                if (column != 0) {
                    for (DefaultMutableTreeTableNode node : values.keySet()) {
                        List<Object> valueList = values.get(node);
                        Object value = valueList.get(column - 1);
                        if (value != null) {
                            return value.getClass();
                        }
                    }
                    return String.class;
                } else {
                    return TreeTableModel.class;
                }
            }

            public int getColumnCount() {
                return columnNames.size() + 1;
            }

            public String getColumnName(int index) {
                String name ;
                if (index == 0) {
                    name = ClientResourceBundle.getString("form.queries.tree");
                } else {
                    name = "<html>" + columnNames.get(index - 1);
                    if (index == sortedCol) {
                        name += isSortAscending ? " ↑" : " ↓";
                    }
                    name += "</html>";
                }
                return name;
            }

            @Override
            public Object getValueAt(Object node, int column) {
                if (column == 0) {
                    return node.toString();
                } else {
                    Object value = values.get(node).get(column - 1);
                    return value != null && value.getClass().equals(String.class) ? value.toString().trim() : value;
                }
            }

            protected boolean isSortAscending = true;
            protected int sortedCol = 0;

            class ColumnListener extends MouseAdapter {
                public void mouseClicked(MouseEvent e) {
                    TableColumnModel colModel = treeTable.getColumnModel();
                    int columnToSort = colModel.getColumnIndexAtX(e.getX());

                    if (sortedCol == columnToSort)
                        isSortAscending = !isSortAscending;
                    else {
                        isSortAscending = true;
                        sortedCol = columnToSort;
                    }
                    if (columnToSort < 1)
                        return;

                    for (int i = 0; i < getColumnCount(); i++) {
                        TableColumn column = colModel.getColumn(i);
                        column.setHeaderValue(getColumnName(column.getModelIndex()));
                    }
                    treeTable.getTableHeader().repaint();

                    DefaultMutableTreeTableNode root = (DefaultMutableTreeTableNode) CustomTreeTableModel.this.root;
                    changeChildrenOrder(root);
                    treeTable.updateUI();
                }

                private void changeChildrenOrder(DefaultMutableTreeTableNode node) {
                    ArrayList<DefaultMutableTreeTableNode> list = Collections.list((Enumeration<DefaultMutableTreeTableNode>) node.children());
                    Collections.sort(list, new NodeComparator(isSortAscending, sortedCol));
                    for (DefaultMutableTreeTableNode child : list) {
                        changeChildrenOrder(child);
                        node.remove(child);
                        node.add(child);
                    }
                }
            }

            class NodeComparator implements Comparator {
                protected boolean isSortAsc;
                protected int columnIndex;

                public NodeComparator(boolean sortAsc, int columnIndex) {
                    isSortAsc = sortAsc;
                    this.columnIndex = columnIndex;
                }

                public int compare(Object o1, Object o2) {
                    Comparable object1 = (Comparable) CustomTreeTableModel.this.getValueAt(o1, columnIndex);
                    Comparable object2 = (Comparable) CustomTreeTableModel.this.getValueAt(o2, columnIndex);
                    if (object1 == null || object2 == null) {
                        return 0;
                    }
                    int result = object1.compareTo(object2);
                    if (!isSortAsc)
                        result = -result;
                    return result;
                }
            }
        }

        private void exportToExcel() throws IOException, WriteException {
            File file = File.createTempFile("tableContents", ".xls");
            WritableWorkbook workbook = Workbook.createWorkbook(file);
            WritableSheet sheet = workbook.createSheet(ClientResourceBundle.getString("form.queries.list1"), 0);

            for (int i = 0; i < treeTableModel.getColumnCount(); i++) {
                String columnName = treeTableModel.getColumnName(i);
                if (i != 0) {
                    columnName = columnName.substring(columnName.indexOf('>') + 1, columnName.lastIndexOf('<'));
                    if (columnName.contains("↑") || columnName.contains("↓")) {
                        columnName = columnName.substring(0, columnName.lastIndexOf(' '));
                    }
                    CellView cv = new CellView();
                    cv.setAutosize(true);
                    sheet.setColumnView(i, cv);
                }
                sheet.addCell(new jxl.write.Label(i, 0, columnName));
            }
            int k = 1;
            for (DefaultMutableTreeTableNode node : treeTableModel.values.keySet()) {
                List<Object> row = treeTableModel.values.get(node);
                for (int i = 0; i <= row.size(); i++) {
                    Object value = treeTableModel.getValueAt(node, i);
                    if (value instanceof Number) {
                        sheet.addCell(new jxl.write.Number(i, k, Double.valueOf(value.toString())));
                    } else if (value instanceof Date) {
                        sheet.addCell(new DateTime(i, k, (Date) value));
                    } else if (value instanceof Boolean) {
                        sheet.addCell(new jxl.write.Boolean(i, k, (Boolean) value));
                    } else {
                        sheet.addCell(new jxl.write.Label(i, k, value == null ? "" : value.toString()));
                    }
                }
                k++;
            }
            workbook.write();
            workbook.close();

            Runtime run = Runtime.getRuntime();
            run.exec("cmd.exe /c start " + file);
        }
    }
}

