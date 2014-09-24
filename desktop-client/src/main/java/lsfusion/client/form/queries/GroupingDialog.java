package lsfusion.client.form.queries;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import jxl.CellView;
import jxl.Workbook;
import jxl.biff.DisplayFormat;
import jxl.format.BorderLineStyle;
import jxl.format.VerticalAlignment;
import jxl.write.*;
import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.base.SystemUtils;
import lsfusion.client.form.ItemAdapter;
import lsfusion.client.form.grid.GridTable;
import lsfusion.client.form.grid.GridTableModel;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientImageClass;
import lsfusion.client.logics.classes.ClientIntegralClass;
import lsfusion.client.logics.classes.ClientType;
import lsfusion.interop.FormGrouping;
import lsfusion.interop.KeyStrokes;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Number;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class GroupingDialog extends JDialog {
    private final int RECORD_QUANTITY_ID = -1;
    
    private List<FormGrouping> savedGroupings;
    
    private GridTable initialTable;
    private GridTableModel initialTableModel;

    private Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> groupChecks = new LinkedHashMap<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox>();
    private Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JSpinner> groupSpinners = new LinkedHashMap<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JSpinner>();
    private Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> groupPivotChecks = new LinkedHashMap<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox>();
    private JCheckBox quantityCheck;
    private Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> sumChecks = new LinkedHashMap<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox>();
    private Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> maxChecks = new LinkedHashMap<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox>();
    
    private JButton removeCurrentGroupingButton = new JButton(getString("form.queries.grouping.remove.grouping"));
    
    private JCheckBox notNullCheck = new JCheckBox(getString("form.queries.grouping.only.filled"));
    private JLabel recordCountLabel = new JLabel();

    private GroupingTreeTable treeTable;
    private JPanel expandButtonsPanel = new JPanel();

    private DefaultComboBoxModel groupsModel = new DefaultComboBoxModel();

    private ChangeListener spinnerListener = new ChangeListener() {
        public void stateChanged(ChangeEvent ce) {
            verifySpinners();
        }
    };

    public GroupingDialog(Frame owner, final GridTable initialTable, final List<FormGrouping> savedGroupings, boolean canBeSaved) throws IOException {
        super(owner, getString("form.queries.grouping"), true);
        this.initialTable = initialTable;
        this.savedGroupings = savedGroupings;
        initialTableModel = initialTable.getTableModel();

        setMinimumSize(new Dimension(800, 400));
        Rectangle bounds = owner.getBounds();
        bounds.x += 20;
        bounds.y += 20;
        bounds.width -= 40;
        bounds.height -= 40;
        setBounds(bounds);
        setMaximumSize(new Dimension(bounds.width, bounds.height));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        ActionListener escListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        };
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        TitledPanel groupByPanel = new TitledPanel(getString("form.queries.grouping.groupby"));
        JPanel allFieldsPanel = new JPanel();
        allFieldsPanel.setLayout(new BoxLayout(allFieldsPanel, BoxLayout.Y_AXIS));

        groupByPanel.setLayout(new BorderLayout());
        groupByPanel.add(allFieldsPanel, BorderLayout.NORTH);

        JScrollPane groupScrollPane = new JScrollPane();
        groupScrollPane.setViewportView(groupByPanel);
        
        JPanel otherChecksPanel = new JPanel();
        otherChecksPanel.setLayout(new BoxLayout(otherChecksPanel, BoxLayout.Y_AXIS));
        quantityCheck = new JCheckBox(getString("form.queries.number.of.entries"));
        otherChecksPanel.add(quantityCheck);

        JPanel valuesChecksPanel = new JPanel(new BorderLayout());
        valuesChecksPanel.add(otherChecksPanel, BorderLayout.NORTH);

        JPanel sumPanel = new JPanel();
        sumPanel.setLayout(new BoxLayout(sumPanel, BoxLayout.Y_AXIS));
        JScrollPane sumScrollPane = new JScrollPane();

        JPanel maxPanel = new JPanel();
        maxPanel.setLayout(new BoxLayout(maxPanel, BoxLayout.Y_AXIS));
        JScrollPane maxScrollPane = new JScrollPane();

        for (int i = 0; i < initialTableModel.getColumnCount(); i++) {
            String columnName = initialTableModel.getColumnName(i).trim(); 
            final JCheckBox checkBox = new JCheckBox(columnName);
            final JCheckBox pivotCheckBox = new JCheckBox(getString("form.queries.grouping.to.column"));

            checkBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Pair<ClientPropertyDraw, ClientGroupObjectValue> column = null;
                    for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> entry : groupChecks.entrySet()) {
                        if (entry.getValue().equals(checkBox)) {
                            column = entry.getKey();
                            break;
                        }
                    }
                    final JSpinner spinner = groupSpinners.get(column);
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
                    invalidate();
                    verifySpinners();
                }
            });
            Pair<ClientPropertyDraw, ClientGroupObjectValue> columnKey = initialTable.getColumnProperty(i);

            groupChecks.put(columnKey, checkBox);
            groupPivotChecks.put(columnKey, pivotCheckBox);
            
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
            spinner.setMinimumSize(new Dimension(35, 19));
            spinner.setPreferredSize(new Dimension(35, 19));
            spinner.addChangeListener(spinnerListener);
            groupSpinners.put(columnKey, spinner);

            if (i != initialTable.getSelectedColumn()) {
                spinner.setVisible(false);
            }

            JPanel fieldPanel = new JPanel();
            fieldPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            fieldPanel.add(checkBox);
            fieldPanel.add(spinner);
            if(SystemUtils.IS_OS_WINDOWS)
                fieldPanel.add(pivotCheckBox);
            fieldPanel.setPreferredSize(new Dimension(fieldPanel.getPreferredSize().width + spinner.getPreferredSize().width, checkBox.getPreferredSize().height + 3));
            allFieldsPanel.add(fieldPanel);

            
            ClientType propertyType = initialTableModel.getColumnProperty(i).baseType; 
            if (propertyType instanceof ClientIntegralClass) {
                JCheckBox checkBoxSum = new JCheckBox(columnName);
                sumChecks.put(columnKey, checkBoxSum);
                JCheckBox checkBoxMax = new JCheckBox(columnName);
                maxChecks.put(columnKey, checkBoxMax);
                sumPanel.add(checkBoxSum);
                maxPanel.add(checkBoxMax);
            } else if (propertyType instanceof ClientImageClass) {
                JCheckBox imageCheckBox = new JCheckBox(columnName);
                maxChecks.put(columnKey, imageCheckBox);
                otherChecksPanel.add(imageCheckBox);
            }
        }

        sumScrollPane.setViewportView(sumPanel);
        maxScrollPane.setViewportView(maxPanel);

        JTabbedPane sumMaxTab = new JTabbedPane();
        sumMaxTab.addTab(getString("form.queries.grouping.sum"), sumScrollPane);
        sumMaxTab.addTab(getString("form.queries.grouping.maximum"), maxScrollPane);
        valuesChecksPanel.add(sumMaxTab);
        
        JButton resetAllButton = new JButton(getString("form.queries.grouping.reset"));
        resetAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetChecks();
            }
        });

        JButton executeButton = new JButton(getString("form.queries.grouping.result"));
        executeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updatePressed();
            }
        });

        JButton pivotButton = new JButton(getString("form.queries.grouping.pivot"));
        pivotButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pivotPressed();
            }
        });

        final JComboBox savedGroupsList = new JComboBox(groupsModel);
        
        // используется с целью обновления состояния при повторном выборе текущей группировки
        savedGroupsList.addPopupMenuListener(new PopupMenuListener() {
            boolean isCancelled = false;

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (!isCancelled) {
                    refreshChecks();
                }
                isCancelled = false;
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                isCancelled = true;
            }
        });
        
        savedGroupsList.addItemListener(new ItemAdapter() {
            @Override
            public void itemSelected(ItemEvent e) {
                if (!savedGroupsList.isPopupVisible()) { // чтобы дважды не обновлять состояние при выборе мышкой
                    refreshChecks();
                }
            }
        });

        refreshGroupingListModel(null);
        savedGroupsList.setMaximumSize(new Dimension(200, 20));
        savedGroupsList.setPreferredSize(new Dimension(200, 20));
        
        removeCurrentGroupingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (groupsModel.getSelectedItem() != null) {
                    savePressed(new FormGrouping((String) groupsModel.getSelectedItem(), initialTable.getGroupObject().getSID(), null, null));
                    
                    savedGroupings.remove(groupsModel.getIndexOf(groupsModel.getSelectedItem()) - 1);
                    refreshGroupingListModel(null);
                }
            }
        });
        
        JButton saveButton = new JButton(getString("form.queries.grouping.save.grouping.as"));
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAsPressed();
            }
        });
        
        TitledPanel saveContainer = new TitledPanel(getString("form.queries.grouping.saved.groupings"));
        JPanel savePanel = new JPanel();
        savePanel.setLayout(new BoxLayout(savePanel, BoxLayout.X_AXIS));
        savePanel.add(savedGroupsList);
        savePanel.add(removeCurrentGroupingButton);
        savePanel.add(saveButton);
        saveContainer.add(savePanel);

        JPanel managementPanel = new JPanel();
        managementPanel.setLayout(new BoxLayout(managementPanel, BoxLayout.X_AXIS));
        managementPanel.add(resetAllButton);
        managementPanel.add(Box.createHorizontalStrut(10));
        managementPanel.add(saveContainer);

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
        labelPanel.add(Box.createHorizontalGlue());
        labelPanel.add(recordCountLabel);
        labelPanel.add(Box.createHorizontalGlue());

        JPanel checkNButtonPanel = new JPanel();
        checkNButtonPanel.setLayout(new BoxLayout(checkNButtonPanel, BoxLayout.X_AXIS));
        checkNButtonPanel.add(notNullCheck);
        if(SystemUtils.IS_OS_WINDOWS)
            checkNButtonPanel.add(pivotButton);
        checkNButtonPanel.add(executeButton);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(labelPanel, BorderLayout.CENTER);
        bottomPanel.add(checkNButtonPanel, BorderLayout.EAST);
        if (canBeSaved) {
            bottomPanel.add(managementPanel, BorderLayout.WEST);
        }

        JSplitPane westPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, groupScrollPane, valuesChecksPanel);
        westPanel.setContinuousLayout(true);
        westPanel.setOneTouchExpandable(true);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(expandButtonsPanel, BorderLayout.WEST);
        JButton excelExport = new JButton(getString("form.queries.grouping.export.to.excel"));
        excelExport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    exportToExcel(false);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        topPanel.add(excelExport, BorderLayout.EAST);

        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BorderLayout());
        eastPanel.add(topPanel, BorderLayout.NORTH);
        JScrollPane treeTableScroll = new JScrollPane();
        eastPanel.add(treeTableScroll, BorderLayout.CENTER);

        JSplitPane allPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westPanel, eastPanel);
        allPanel.setContinuousLayout(true);
        allPanel.setOneTouchExpandable(true);
        add(bottomPanel, BorderLayout.SOUTH);
        add(allPanel, BorderLayout.CENTER);

        treeTable = new GroupingTreeTable();

        treeTableScroll.setViewportView(treeTable);

        refreshChecks();
    }

    protected abstract void savePressed(FormGrouping grouping);
    public abstract void updatePressed();
    public abstract void pivotPressed();

    private void verifySpinners() {
        int maxValue = getMaxSpinnerValue();
        for (int i = 1; i <= maxValue; i++) {
            boolean decrease = true;
            for (JSpinner spinner : groupSpinners.values()) {
                if (spinner.isVisible() && (Integer) spinner.getValue() == i) {
                    decrease = false;
                    break;
                }
            }
            if (decrease) {
                for (JSpinner spinner : groupSpinners.values()) {
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
        for (JSpinner spinner : groupSpinners.values()) {
            if (spinner.isVisible() && (Integer) spinner.getValue() > maxValue) {
                maxValue = (Integer) spinner.getValue();
            }
        }
        return maxValue;
    }

    public List<Map<Integer, List<byte[]>>> getSelectedGroupLevels() {
        List<Map<Integer, List<byte[]>>> selectedGroupProperties = new ArrayList<Map<Integer, List<byte[]>>>();
        List<Integer> level = new ArrayList<Integer>();
        for (int k = 1; k <= getMaxSpinnerValue(); k++) {
            List<Integer> newLevel = new ArrayList<Integer>(level);
            int i = 0;
            for (JSpinner spinner : groupSpinners.values()) {
                if (spinner.isVisible() && (((Integer) spinner.getValue() == k))) {
                    newLevel.add(i);
                }
                i++;
            }
            level = new ArrayList<Integer>(newLevel);

            Map<Integer, List<byte[]>> groupLevel = new OrderedMap<Integer, List<byte[]>>();
            for (Integer index : newLevel) {
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
        Map<Integer, List<byte[]>> selectedSumMap = new LinkedHashMap<Integer, List<byte[]>>();
        if (quantityCheck.isSelected()) {
            selectedSumMap.put(RECORD_QUANTITY_ID, null);
        }
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> entry : sumChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                int propertyID = entry.getKey().first.getID();
                List<byte[]> list = selectedSumMap.get(propertyID);
                if (list == null) {
                    list = new ArrayList<byte[]>();
                }
                list.add(entry.getKey().second.serialize());
                selectedSumMap.put(propertyID, list);
            }
        }
        
        return selectedSumMap;
    }

    public LinkedHashMap<Integer, Boolean> getSelectedPivotColumns() {
        LinkedHashMap<Integer, Boolean> selectedPivotColumns = new LinkedHashMap<Integer, Boolean>();
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JSpinner> spinner : groupSpinners.entrySet()) {
            if (spinner.getValue().isVisible()) {
                selectedPivotColumns.put((Integer)spinner.getValue().getValue(), groupPivotChecks.get(spinner.getKey()).isSelected());
            }            
        }
        return selectedPivotColumns;
    }

    public int getSelectedPivotDataFieldsCount() {
        int count = 0;
        if(quantityCheck.isSelected()) {
            count++;
        }
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> entry : sumChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                count++;
            }
        }                        
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> entry : maxChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                count++;
            }
        }
        return count;
    }

    public Map<Integer, List<byte[]>> getSelectedMaxMap() {
        Map<Integer, List<byte[]>> selectedMaxMap = new LinkedHashMap<Integer, List<byte[]>>();
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> entry : maxChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                int propertyID = entry.getKey().first.getID();
                List<byte[]> list = selectedMaxMap.get(propertyID);
                if (list == null) {
                    list = new ArrayList<byte[]>();
                }
                list.add(entry.getKey().second.serialize());
                selectedMaxMap.put(propertyID, list);
            }
        }
        return selectedMaxMap;
    }

    public boolean onlyNotNull() {
        return notNullCheck.isSelected();
    }
    
    private void resetChecks() {
        for (JCheckBox check : groupChecks.values()) {
            check.setSelected(false);
        }
        for (JSpinner spinner : groupSpinners.values()) {
            spinner.setVisible(false);
        }
        for(JCheckBox pivotCheck : groupPivotChecks.values()) {
            pivotCheck.setSelected(false);
        }
        quantityCheck.setSelected(false);
        for (JCheckBox box : sumChecks.values())
            box.setSelected(false);
        for (JCheckBox box : maxChecks.values())
            box.setSelected(false);    
    }
    
    private void refreshChecks() {
        resetChecks();

        String selectedName = (String) groupsModel.getSelectedItem();
        if (selectedName == null) {
            quantityCheck.setSelected(true);
            int columnIndex = initialTable.getSelectedColumn();
            if (columnIndex == -1) {     //для пустых гридов. указываем выбранным по умолчанию первое свойство
                columnIndex = 0;
            }
            Pair<ClientPropertyDraw, ClientGroupObjectValue> columnKey = initialTable.getColumnProperty(columnIndex);
            groupChecks.get(columnKey).setSelected(true);
            groupSpinners.get(columnKey).setVisible(true);
        } else {
            FormGrouping grouping = null;
            for (FormGrouping gr : savedGroupings) {
                if (selectedName.equals(gr.name)) {
                    grouping = gr;
                    break;
                }
            }
            
            if (grouping != null) {
                if (grouping.showItemQuantity != null && grouping.showItemQuantity) {
                    quantityCheck.setSelected(true);
                }
                
                List<Pair<Integer, Pair<ClientPropertyDraw, ClientGroupObjectValue>>> orders = new ArrayList<Pair<Integer, Pair<ClientPropertyDraw, ClientGroupObjectValue>>>(); 
                for (FormGrouping.PropertyGrouping propGrouping : grouping.propertyGroupings) {
                    for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> checkEntry : groupChecks.entrySet()) {
                        if (checkEntry.getKey().first.getSID().equals(propGrouping.propertySID)) {
                            if (propGrouping.groupingOrder != null) {
                                orders.add(new Pair<Integer, Pair<ClientPropertyDraw, ClientGroupObjectValue>>(propGrouping.groupingOrder, checkEntry.getKey()));        
                            }
                            if (propGrouping.pivot != null && propGrouping.pivot) {
                                groupPivotChecks.get(checkEntry.getKey()).setSelected(true);
                            }
                            if (propGrouping.sum != null && propGrouping.sum) {
                                sumChecks.get(checkEntry.getKey()).setSelected(true);    
                            }
                            if (propGrouping.max != null && propGrouping.max) {
                                maxChecks.get(checkEntry.getKey()).setSelected(true);
                            }
                            // не выходим из цикла, чтобы вся группа в колонках была проставлена, если нужно
                        }
                    }
                }
                
                Collections.sort(orders, new Comparator<Pair<Integer, Pair<ClientPropertyDraw, ClientGroupObjectValue>>>() {
                    @Override
                    public int compare(Pair<Integer, Pair<ClientPropertyDraw, ClientGroupObjectValue>> o1, Pair<Integer, Pair<ClientPropertyDraw, ClientGroupObjectValue>> o2) {
                        return o1.first - o2.first;
                    }
                });
                
                for (Pair<Integer, Pair<ClientPropertyDraw, ClientGroupObjectValue>> item : orders) {
                    groupChecks.get(item.second).setSelected(true);
                    JSpinner spinner = groupSpinners.get(item.second);
                    spinner.setVisible(true);
                    spinner.setValue(item.first);
                }
                verifySpinners(); // на всякий случай. вдруг порядки будут непоследовательны
            }
        }
    }

    public void update(List<Map<List<Object>, List<Object>>> values) {
        List<ClientPropertyDraw> columnProperties = new ArrayList<ClientPropertyDraw>();
        List<String> names = new ArrayList<String>();
        
        int lastLevelColumnCount = 0;
        for (int i = 1; i <= getMaxSpinnerValue(); i++) {
            int index = 0;
            for (JSpinner spinner : groupSpinners.values()) {
                if (spinner.isVisible() && (Integer) spinner.getValue() == i) {
                    ClientPropertyDraw property = initialTableModel.getColumnProperty(index);
                    columnProperties.add(property);
                    names.add(initialTableModel.getColumnName(index).trim());
                    
                    lastLevelColumnCount++;
                }
                index++;
            }
        }
        if (quantityCheck.isSelected()) {
            columnProperties.add(null);
            names.add(getString("form.queries.grouping.nodes"));
        }
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> entry : sumChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                ClientPropertyDraw property = entry.getKey().first;
                columnProperties.add(property);
                names.add(groupChecks.get(entry.getKey()).getText() + " [S]");
            }
        }

        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JCheckBox> entry : maxChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                ClientPropertyDraw property = entry.getKey().first;
                columnProperties.add(property);
                names.add(groupChecks.get(entry.getKey()).getText() + (property.baseType instanceof ClientImageClass ? "" : " [M]"));
            }
        }

        treeTable.updateModel(lastLevelColumnCount, columnProperties, names, values);

        createExpandButtons();
        recordCountLabel.setText(getString("form.queries.grouping.total.nodes")+" " + treeTable.getLastLevelRowCount());
    }

    private void createExpandButtons() {
        expandButtonsPanel.removeAll();
        expandButtonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        final List<JButton> buttons = new ArrayList<JButton>();
        for (int i = 0; i <= treeTable.getLevelCount() - 1; i++) {
            JButton button = new JButton("" + i);
            if (i == 0) {
                button.setText("-");
                button.setToolTipText(getString("form.queries.grouping.collapse.all"));
            } else {
                button.setToolTipText(getString("form.queries.grouping.expand.level")+" " + i);
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
                    expand(null, treeTable.getRoot());
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

    private File exportToExcel(boolean isPivot) throws IOException, WriteException {
        File file = File.createTempFile("tableContents", ".xls");
        WritableWorkbook workbook = Workbook.createWorkbook(file);
        WritableSheet sheet = workbook.createSheet(getString("form.queries.grouping.sheet1"), 0);

        addExcelSubrows(sheet, 1, (GroupingTreeTable.SortableTreeTableNode) treeTable.getRoot(), new int[treeTable.getColumnCount()]);

        for (int i = 0; i < treeTable.getColumnCount(); i++) {
            String columnName = treeTable.getColumnName(i);
            if (i != 0) {
                columnName = columnName.substring(columnName.indexOf('>') + 1, columnName.lastIndexOf('<'));
                if (columnName.contains("↑") || columnName.contains("↓")) {
                    columnName = columnName.substring(0, columnName.lastIndexOf(' '));
                }
            }
            WritableCellFormat cellFormat = createCellFormat(null, true);
            cellFormat.setWrap(true);
            cellFormat.setVerticalAlignment(VerticalAlignment.CENTRE);
            sheet.addCell(new jxl.write.Label(i, 0, columnName, cellFormat));
        }
               
        workbook.write();
        workbook.close();

        if(!isPivot)
            Runtime.getRuntime().exec("cmd.exe /c start " + file);
        
        return file;
    }

    public void exportToExcelPivot() {
        try {
            File file = exportToExcel(true);

            Integer xlRowField = 1;
            Integer xlColumnField = 2;
            Integer xlFilterField = 3;
            Integer xlDataField = 4;

            Integer xlSum = -4157;
            Integer xlCount = -4112;
            Integer xlAverage = -4106;
            
            ActiveXComponent excelComponent = new ActiveXComponent("Excel.Application");

            Dispatch workbooks = excelComponent.getProperty("Workbooks").toDispatch();
            Dispatch workbook = Dispatch.call(workbooks, "Open", file.getAbsolutePath()).toDispatch();
            
            Dispatch sourceSheet = Dispatch.get(workbook, "ActiveSheet").toDispatch();
            Dispatch sheets = Dispatch.get(workbook, "Worksheets").toDispatch();
            Dispatch destinationSheet = Dispatch.get(sheets, "Add").toDispatch();
            Dispatch.put(destinationSheet, "Name", "PivotTable");

            String lastCell = getCellIndex(treeTable.getColumnCount(), treeTable.getRowCount()==0 ? 2 : (treeTable.getRowCount()) + 1);
            Dispatch sourceDataNativePeer = Dispatch.invoke(sourceSheet, "Range", Dispatch.Get, new Object[]{"A1:" + lastCell}, new int[1]).toDispatch();
            Dispatch destinationNativePeer = Dispatch.invoke(destinationSheet, "Range", Dispatch.Get, new Object[]{"A1"}, new int[1]).toDispatch();

            Variant unspecified = Variant.VT_MISSING;
            Dispatch pivotTableWizard = Dispatch.invoke(workbook, "PivotTableWizard", Dispatch.Get, new Object[]{new Variant(1),  //SourceType
                    new Variant(sourceDataNativePeer), //SourceData
                    new Variant(destinationNativePeer), //TableDestination
                    new Variant("PivotTable"), //TableName
                    new Variant(false), //RowGrand
                    new Variant(false), //ColumnGrand
                    new Variant(true), //SaveData
                    new Variant(true), //HasAutoFormat
                    unspecified, //AutoPage
                    unspecified, //Reserved
                    new Variant(false), //BackgroundQuery
                    new Variant(false), //OptimizeCache
                    new Variant(1), //PageFieldOrder
                    unspecified, //PageFieldWrapCount
                    unspecified, //ReadData
                    unspecified //Connection
            }, new int[1]).toDispatch();


            LinkedHashMap<Integer, Boolean> pivotColumns = getSelectedPivotColumns();            
            int pivotDataFieldsCount = getSelectedPivotDataFieldsCount();
            
            for (int i = pivotDataFieldsCount; i > 0; i--) {
                Dispatch fieldDispatch = Dispatch.call(pivotTableWizard, "HiddenFields", new Variant(i + pivotColumns.size() + 1)).toDispatch();
                Dispatch.put(fieldDispatch, "Orientation", new Variant(xlDataField));
                Dispatch.put(fieldDispatch, "Function", new Variant(xlSum));
                String caption = Dispatch.get(fieldDispatch, "Caption").getString().replace("Сумма по полю ", "");
                Dispatch.put(fieldDispatch, "Caption", new Variant(caption + "*"));
            }

            for (int i = pivotColumns.size(); i > 0; i--) {
                Dispatch fieldDispatch = Dispatch.call(pivotTableWizard, "HiddenFields", new Variant(i + 1)).toDispatch();
                Dispatch.put(fieldDispatch, "Orientation", new Variant(pivotColumns.get(i) ? xlColumnField : xlRowField));
            }

            Dispatch field = Dispatch.get(pivotTableWizard, "DataPivotField").toDispatch();
            if (pivotDataFieldsCount > 1)
                Dispatch.put(field, "Orientation", new Variant(xlColumnField));

            Dispatch.get(workbook, "Save");
            Dispatch.call(workbooks, "Close");                                                    
            excelComponent.invoke("Quit", new Variant[0]);
            ComThread.Release();

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getCellIndex(int column, int row) {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String columnIndex = "";
        while (column > 0) {
            columnIndex = letters.charAt((column-1) % 26) + columnIndex;
            column = (column - 1) / 26;
        }
        return columnIndex + row;
    }

    private WritableCellFormat createCellFormat(DisplayFormat displayFormat, boolean bold) throws WriteException {
        WritableCellFormat format;
        if (bold) {
            WritableFont font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
            format = displayFormat != null ? new WritableCellFormat(font, displayFormat) : new WritableCellFormat(font);
        } else {
            format = displayFormat != null ? new WritableCellFormat(displayFormat) : new WritableCellFormat();
        }
        format.setBorder(jxl.format.Border.ALL, BorderLineStyle.THIN);
        return format;
    }
    
    private int addExcelSubrows(WritableSheet sheet, int currentRow, GroupingTreeTable.SortableTreeTableNode parent, int[] maxWidth) throws WriteException {
        if (parent.getParent() != null) {
            List<Object> row = treeTable.getRow(parent);
            for (int column = 0; column <= row.size(); column++) {
                Object value = treeTable.getValueAt(parent, column);
                int length = 0;
                if (value instanceof BigDecimal || value instanceof Double) {
                    length = new DecimalFormat("#,##0.00").format(value).length();
                    sheet.addCell(new jxl.write.Number(column, currentRow, Double.valueOf(value.toString()), createCellFormat(NumberFormats.THOUSANDS_FLOAT, false)));    
                } else if (value instanceof Number) {
                    length = new DecimalFormat("#,##0").format(value).length();
                    sheet.addCell(new jxl.write.Number(column, currentRow, Double.valueOf(value.toString()), createCellFormat(NumberFormats.THOUSANDS_INTEGER, false)));
                } else if (value instanceof Time) {
                    length = new SimpleDateFormat("H:mm:ss").format(value).length();
                    sheet.addCell(new jxl.write.DateTime(column, currentRow, (Date) value, createCellFormat(DateFormats.FORMAT8, false)));
                } else if (value instanceof Date) {
                    length = new SimpleDateFormat("M/d/yy").format(value).length();
                    sheet.addCell(new jxl.write.DateTime(column, currentRow, (Date) value, createCellFormat(DateFormats.DEFAULT, false)));
                } else if (value instanceof Boolean) {
                    length = value.toString().length();
                    sheet.addCell(new jxl.write.Boolean(column, currentRow, (Boolean) value, createCellFormat(null, false)));
                } else if (value instanceof byte[]) { // здесь ожидается изображение
                    WritableCellFormat format = new WritableCellFormat();
                    format.setBorder(jxl.format.Border.ALL, BorderLineStyle.THIN);
                    sheet.getWritableCell(column, currentRow).setCellFormat(format);
                    sheet.addCell(new Blank(column, currentRow, format));
                    sheet.setRowView(currentRow, 500);

                    WritableImage image = new WritableImage(column, currentRow, 1, 1, (byte[]) value);
                    image.setImageAnchor(WritableImage.MOVE_AND_SIZE_WITH_CELLS);
                    sheet.addImage(image);
                } else {
                    length = value == null ? 0 : value.toString().length();
                    sheet.addCell(new jxl.write.Label(column, currentRow, value == null ? "" : value.toString(), createCellFormat(null, false)));
                }
                
                if(maxWidth[column]<length){
                    maxWidth[column] = length;
                    CellView cv = new CellView();
                    cv.setSize(256 * Math.max(10, length));
                    sheet.setColumnView(column, cv);
                }                
            }
            currentRow++;
        }
        
        int parentRow = currentRow;
        Enumeration<? extends MutableTreeTableNode> children = parent.children();
        while (children.hasMoreElements()) {
            GroupingTreeTable.SortableTreeTableNode child = (GroupingTreeTable.SortableTreeTableNode) children.nextElement();
            
            currentRow = addExcelSubrows(sheet, currentRow, child, maxWidth);
        }
        if (parent.children().hasMoreElements() && parent.getParent() != null) {
            sheet.setRowGroup(parentRow , currentRow - 1, false);
        }
        
        return currentRow;
    }

    private void refreshGroupingListModel(FormGrouping itemToSelect) {
        removeCurrentGroupingButton.setVisible(savedGroupings.size() > 0);
        
        groupsModel.removeAllElements();
        groupsModel.addElement(null);
        for (FormGrouping grouping : savedGroupings) {
            groupsModel.addElement(grouping.name);
        }
            
        groupsModel.setSelectedItem(itemToSelect != null ? itemToSelect.name : null);
    }
    
    private void saveAsPressed() {
        final JDialog saveAsDialog = new JDialog(this, getString("form.queries.grouping.save.grouping.as"), true);
        saveAsDialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        ActionListener escListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                saveAsDialog.dispose();
            }
        };
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        saveAsDialog.getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        JLabel enterName = new JLabel(getString("form.queries.grouping.save.choose.name"));
        final JComboBox nameBox = new JComboBox();
        for (int i = 1; i < groupsModel.getSize(); i++) {
            nameBox.addItem(groupsModel.getElementAt(i));
        }
        nameBox.setPreferredSize(new Dimension(200, 20));
        nameBox.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        nameBox.setEditable(true);
        
        nameBox.setSelectedItem(groupsModel.getSelectedItem());
        
        nameBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyStrokes.isEnterEvent(e)) {
                    String name = nameBox.getEditor().getItem().toString();
                    if (!name.isEmpty()) {
                        saveByName(name);
                    }
                    saveAsDialog.dispose();    
                } else {
                    super.keyTyped(e);
                }
            }
        });
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        boxPanel.add(Box.createVerticalGlue());
        boxPanel.add(nameBox);
        boxPanel.add(Box.createVerticalGlue());
        
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        namePanel.add(enterName, BorderLayout.WEST);
        namePanel.add(boxPanel, BorderLayout.CENTER);
        mainPanel.add(namePanel, BorderLayout.CENTER);
        
        JButton saveButton = new JButton(getString("form.queries.grouping.save"));
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nameBox.getSelectedItem() != null) {
                    String name = nameBox.getSelectedItem().toString();
                    if (!name.isEmpty()) {
                        saveByName(name);
                    }
                }
                saveAsDialog.dispose();
            }
        });
        JButton cancelButton = new JButton(getString("form.queries.grouping.save.cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAsDialog.dispose();
            }
        });
        
        JPanel borderButtonsPanel = new JPanel(new BorderLayout());
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(saveButton);
        buttonsPanel.add(cancelButton);
        borderButtonsPanel.add(buttonsPanel, BorderLayout.EAST);
        
        mainPanel.add(borderButtonsPanel, BorderLayout.SOUTH);
        
        saveAsDialog.add(mainPanel);
        
        saveAsDialog.pack();
        saveAsDialog.setLocationRelativeTo(this);
        saveAsDialog.setMinimumSize(saveAsDialog.getSize());

        nameBox.getEditor().selectAll();
        
        saveAsDialog.setVisible(true);
    }
    
    private void saveByName(String name) {
        List<FormGrouping.PropertyGrouping> props = new ArrayList<FormGrouping.PropertyGrouping>();
        FormGrouping grouping = new FormGrouping(name, initialTable.getGroupObject().getSID(), quantityCheck.isSelected() ? true : null, null);
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, JSpinner> entry : groupSpinners.entrySet()) {
            Integer order = entry.getValue().isVisible() ? (Integer) entry.getValue().getValue() : null;
            JCheckBox sumBox = sumChecks.get(entry.getKey());
            Boolean sum = (sumBox != null && sumBox.isSelected()) ? true : null;
            JCheckBox maxBox = maxChecks.get(entry.getKey());
            Boolean max = (maxBox != null && maxBox.isSelected()) ? true : null;
            JCheckBox pivotBox = groupPivotChecks.get(entry.getKey());
            Boolean pivot = (pivotBox != null && pivotBox.isSelected()) ? true : null;
            if (!isAlreadyInList(props, entry.getKey().first.getSID()) || order != null) { // хоть какая-то определённость для групп в колонках
                                                                                           // сохраняем порядок, если хоть одна выбрана
                props.add(grouping.new PropertyGrouping(entry.getKey().first.getSID(), order, sum, max, pivot));
            }
        }

        grouping.propertyGroupings = props;
        
        FormGrouping sameNameGrouping = null;
        for (FormGrouping gr : savedGroupings) {
            if (name.equals(gr.name)) {
                sameNameGrouping = gr;
            }
        }
        if (sameNameGrouping != null) {
            int index = savedGroupings.indexOf(sameNameGrouping);
            savedGroupings.remove(sameNameGrouping);
            savedGroupings.add(index, grouping);
        } else {
            savedGroupings.add(grouping);
        }

        refreshGroupingListModel(grouping);
        
        savePressed(grouping);    
    }
    
    private boolean isAlreadyInList(List<FormGrouping.PropertyGrouping> props, String propertySID) {
        for (FormGrouping.PropertyGrouping grouping : props) {
            if (grouping.propertySID.equals(propertySID)) {
                return true;
            }
        }
        return false;
    }
}