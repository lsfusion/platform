package lsfusion.client.form.grid;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.ArrayListTransferHandler;
import lsfusion.client.Main;
import lsfusion.client.form.GroupObjectController;
import lsfusion.client.form.RmiQueue;
import lsfusion.client.form.queries.TitledPanel;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.FontInfo;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class UserPreferencesDialog extends JDialog {
    
    private GroupObjectController goController;
    private GridTable initialTable;

    private UserPreferencesPropertyListModel visibleListModel, invisibleListModel;
    private JList<UserPreferencesPropertyListItem> visibleList, invisibleList;
    private JTextField fontSizeField;
    private JTextField pageSizeField;
    private JTextField headerHeightField;
    private JCheckBox isFontBoldCheckBox;
    private JCheckBox isFontItalicCheckBox;
    private JTextField columnCaptionField;
    private JTextField columnPatternField;

    public UserPreferencesDialog(Frame owner, final GridTable initialTable, GroupObjectController goController, final boolean canBeSaved) throws IOException {
        super(owner, getString("form.grid.preferences"), true);
        this.initialTable = initialTable;
        this.goController = goController;

        setMinimumSize(new Dimension(500, 500));
        setBounds(new Rectangle(100, 100, 500, 500));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        final JPanel allFieldsPanel = new JPanel();
        allFieldsPanel.setLayout(new BoxLayout(allFieldsPanel, BoxLayout.Y_AXIS));

        ActionListener escListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        };
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        ArrayListTransferHandler arrayListHandler = new ArrayListTransferHandler();

        visibleListModel = new UserPreferencesPropertyListModel(true);
        invisibleListModel = new UserPreferencesPropertyListModel(false);
        visibleList = new JList<>(visibleListModel);
        invisibleList = new JList<>(invisibleListModel);
        
        TitledPanel visiblePanel = initColumnsList(visibleList, true, arrayListHandler);
        TitledPanel invisiblePanel = initColumnsList(invisibleList, false, arrayListHandler);

        JPanel columnsPanel = new JPanel();
        columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.X_AXIS));
        columnsPanel.add(visiblePanel);
        columnsPanel.add(createArrowsPanel(), BoxLayout.Y_AXIS);
        columnsPanel.add(invisiblePanel);


        final JButton applyButton = new JButton(getString("form.grid.preferences.save.settings"));
        applyButton.addActionListener(createApplyResetButtonListener(true, false));

        final JButton resetButton = new JButton(getString("form.grid.preferences.reset.settings"));
        resetButton.addActionListener(createApplyResetButtonListener(false, false));

        TitledPanel currentUserPanel = new TitledPanel(getString("form.grid.preferences.for.user"));
        currentUserPanel.add(applyButton, BorderLayout.NORTH);
        currentUserPanel.add(resetButton, BorderLayout.SOUTH);

        columnCaptionField = new JTextField(30);
        columnCaptionField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateColumnName();
            }
            public void removeUpdate(DocumentEvent e) {
                updateColumnName();
            }
            public void insertUpdate(DocumentEvent e) {
                updateColumnName();
            }

            public void updateColumnName() {
                UserPreferencesPropertyListItem selectedVisibleValue = visibleList.getSelectedValue();
                UserPreferencesPropertyListItem selectedInvisibleValue = invisibleList.getSelectedValue();
                String userCaption = BaseUtils.nullEmpty(columnCaptionField.getText());
                if (selectedInvisibleValue == null && selectedVisibleValue != null) {
                    selectedVisibleValue.setUserCaption(userCaption);
                    visibleList.update(visibleList.getGraphics());
                } else if (selectedVisibleValue == null && selectedInvisibleValue != null) {
                    selectedInvisibleValue.setUserCaption(userCaption);
                    invisibleList.update(invisibleList.getGraphics());
                }
            }
        });

        columnPatternField = new JTextField(30);
        columnPatternField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateColumnPattern();
            }
            public void removeUpdate(DocumentEvent e) {
                updateColumnPattern();
            }
            public void insertUpdate(DocumentEvent e) {
                updateColumnPattern();
            }

            public void updateColumnPattern() {
                UserPreferencesPropertyListItem selectedVisibleValue = visibleList.getSelectedValue();
                UserPreferencesPropertyListItem selectedInvisibleValue = invisibleList.getSelectedValue();
                String userPattern = BaseUtils.nullEmpty(columnPatternField.getText());
                if (selectedInvisibleValue == null && selectedVisibleValue != null) {
                    selectedVisibleValue.setUserPattern(userPattern);
                } else if (selectedVisibleValue == null && selectedInvisibleValue != null) {
                    selectedInvisibleValue.setUserPattern(userPattern);
                }
            }
        });

        TitledPanel columnSettingsPanel = new TitledPanel(getString("form.grid.preferences.selected.column.settings"));
        columnSettingsPanel.setLayout(new BoxLayout(columnSettingsPanel, BoxLayout.Y_AXIS));//FlowLayout(FlowLayout.CENTER));

        JPanel columnCaptionPanel = new JPanel();
        columnCaptionPanel.add(new JLabel(getString("form.grid.preferences.column.caption") + ": "));
        columnCaptionPanel.add(columnCaptionField);
        columnSettingsPanel.add(columnCaptionPanel);

        JPanel columnPatternPanel = new JPanel();
        columnPatternPanel.add(new JLabel(getString("form.grid.preferences.column.pattern") + ": "));
        columnPatternPanel.add(columnPatternField);
        columnSettingsPanel.add(columnPatternPanel);

        TitledPanel gridSettingsPanel = new TitledPanel(getString("form.grid.preferences.grid.settings"));
        gridSettingsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        gridSettingsPanel.add(new JLabel(getString("form.grid.preferences.page.size") + ": "));
        pageSizeField = new JTextField(4);
        gridSettingsPanel.add(pageSizeField);
        gridSettingsPanel.add(new JLabel(getString("form.grid.preferences.header.height") + ": "));
        headerHeightField = new JTextField(4);
        gridSettingsPanel.add(headerHeightField);

        
        fontSizeField = new JTextField(2);
        isFontBoldCheckBox = new JCheckBox(getString("descriptor.editor.font.style.bold"));
        isFontItalicCheckBox = new JCheckBox(getString("descriptor.editor.font.style.italic"));
        
        TitledPanel fontPanel = new TitledPanel(getString("form.grid.preferences.font.settings"));
        fontPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        fontPanel.add(new JLabel(getString("descriptor.editor.font.size") + ": "));
        fontPanel.add(fontSizeField);
        fontPanel.add(isFontBoldCheckBox);
        fontPanel.add(isFontItalicCheckBox);

        Box gridColumnFontSettingsPanel = new Box(BoxLayout.Y_AXIS);
        gridColumnFontSettingsPanel.add(gridSettingsPanel);
        gridColumnFontSettingsPanel.add(columnSettingsPanel);
        gridColumnFontSettingsPanel.add(fontPanel);
        
        JPanel applyResetButtonsPanel = new JPanel();
        applyResetButtonsPanel.add(currentUserPanel, BorderLayout.WEST);

        if (Main.configurationAccessAllowed) {
            final JButton applyForAllButton = new JButton(getString("form.grid.preferences.save.settings"));
            applyForAllButton.addActionListener(createApplyResetButtonListener(true, true));

            final JButton resetForAllButton = new JButton(getString("form.grid.preferences.reset.settings"));
            resetForAllButton.addActionListener(createApplyResetButtonListener(false, true));

            TitledPanel allUsersPanelPanel = new TitledPanel(getString("form.grid.preferences.for.all.users"));
            allUsersPanelPanel.add(applyForAllButton, BorderLayout.NORTH);
            allUsersPanelPanel.add(resetForAllButton, BorderLayout.SOUTH);
            applyResetButtonsPanel.add(allUsersPanelPanel, BorderLayout.EAST);
        }

        Box settingsAndApplyResetPanel = new Box(BoxLayout.Y_AXIS);
        settingsAndApplyResetPanel.add(gridColumnFontSettingsPanel);
        if (canBeSaved) {
            settingsAndApplyResetPanel.add(applyResetButtonsPanel);
        }

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButtonPressed();
                firePropertyChange("buttonPressed", null, null);
            }
        });

        JButton cancelButton = new JButton(getString("form.grid.preferences.cancel"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(settingsAndApplyResetPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonsPanel, BorderLayout.EAST);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(columnsPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
        
        refreshValues(mergeFont());
    }

    private TitledPanel initColumnsList(final JList<UserPreferencesPropertyListItem> list, final boolean visible, ArrayListTransferHandler arrayListHandler) {
        final UserPreferencesPropertyListModel listModel = (UserPreferencesPropertyListModel) list.getModel();
        final JList<UserPreferencesPropertyListItem> anotherList = visible ? invisibleList : visibleList;
        final UserPreferencesPropertyListModel anotherListModel = (UserPreferencesPropertyListModel) anotherList.getModel();

        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setTransferHandler(arrayListHandler);
        list.setDragEnabled(true);

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!list.isFocusable())
                    list.setFocusable(true);
                boolean hasFocus = list.hasFocus() || list.requestFocusInWindow();
                if (hasFocus) {
                    UserPreferencesPropertyListItem selectedValue = list.getSelectedValue();
                    if (selectedValue != null) {
                        String newText = selectedValue.getUserCaption(true);
                        if (!columnCaptionField.getText().equals(newText)) {
                            columnCaptionField.setText(newText);
                        }
                        String newPattern = getItemPattern(selectedValue);
                        if (!columnPatternField.getText().equals(newPattern)) {
                            columnPatternField.setText(newPattern);
                        }
                    }
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                anotherList.clearSelection();
                list.requestFocusInWindow();
                if (e.getClickCount() == 2) {
                    Rectangle r = list.getCellBounds(0, list.getLastVisibleIndex());
                    if (r != null && r.contains(e.getPoint())) {
                        int index = list.locationToIndex(e.getPoint());
                        anotherListModel.addElement(listModel.get(index));
                        listModel.remove(index);
                        clearColumnFields();
                    }
                }
            }
        });

        list.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                UserPreferencesPropertyListItem selectedValue = list.getSelectedValue();
                if (selectedValue != null) {
                    String caption = selectedValue.getUserCaption(true);
                    String pattern = getItemPattern(selectedValue);
                    if (caption != null && !columnCaptionField.getText().equals(caption))
                        columnCaptionField.setText(caption);
                    if (pattern != null && !columnPatternField.getText().equals(pattern))
                        columnPatternField.setText(pattern);
                }
            }
        });

        JScrollPane visibleListView = new JScrollPane(list);
        list.setCellRenderer(new UserPreferencesListItemRenderer(true));
        visibleListView.setPreferredSize(new Dimension(200, 100));
        TitledPanel visiblePanel = new TitledPanel(getString(visible ? "form.grid.preferences.displayed.columns" : "form.grid.preferences.hidden.columns"));
        visiblePanel.setLayout(new BorderLayout());
        visiblePanel.add(visibleListView, BorderLayout.CENTER);
        return visiblePanel;
    }

    private JPanel createArrowsPanel() {
        JButton showSelectedButton = new JButton(new ImageIcon(Main.class.getResource("/images/arrowLeftOne.png")));
        showSelectedButton.setBorder(null);
        showSelectedButton.addActionListener(createMoveSelectedButtonListener(invisibleList, visibleListModel));

        JButton hideSelectedButton = new JButton(new ImageIcon(Main.class.getResource("/images/arrowRightOne.png")));
        hideSelectedButton.setBorder(null);
        hideSelectedButton.addActionListener(createMoveSelectedButtonListener(visibleList, invisibleListModel));

        JButton showAllButton = new JButton(new ImageIcon(Main.class.getResource("/images/arrowLeft.png")));
        showAllButton.setBorder(null);
        showAllButton.addActionListener(createMoveAllButtonListener(invisibleListModel, visibleListModel));

        JButton hideAllButton = new JButton(new ImageIcon(Main.class.getResource("/images/arrowRight.png")));
        hideAllButton.setBorder(null);
        hideAllButton.addActionListener(createMoveAllButtonListener(visibleListModel, invisibleListModel));

        JPanel arrowsPanel = new JPanel();
        arrowsPanel.setLayout(new BoxLayout(arrowsPanel, BoxLayout.Y_AXIS));
        arrowsPanel.add(hideSelectedButton);
        arrowsPanel.add(showSelectedButton);
        arrowsPanel.add(Box.createVerticalStrut(10));
        arrowsPanel.add(hideAllButton);
        arrowsPanel.add(showAllButton);
        return arrowsPanel;
    }

    private ActionListener createMoveSelectedButtonListener(final JList<UserPreferencesPropertyListItem> fromList, final UserPreferencesPropertyListModel toModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<UserPreferencesPropertyListItem> selectedValuesList = fromList.getSelectedValuesList();
                if (!selectedValuesList.isEmpty()) {
                    for (UserPreferencesPropertyListItem listItem : selectedValuesList) {
                        toModel.addElement(listItem);
                        UserPreferencesPropertyListModel fromModel = (UserPreferencesPropertyListModel) fromList.getModel();
                        fromModel.remove(fromModel.indexOf(listItem));
                    }
                    clearColumnFields();
                }
            }
        };
    }

    private ActionListener createMoveAllButtonListener(final UserPreferencesPropertyListModel fromModel, final UserPreferencesPropertyListModel toModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < fromModel.getSize(); i++) {
                    toModel.addElement(fromModel.get(i));
                }
                fromModel.clear();
                clearColumnFields();
            }
        };
    }

    private ActionListener createApplyResetButtonListener(final boolean apply, final boolean forAllUsers) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        if (apply) {
                            applyButtonPressed(forAllUsers);
                        } else {
                            resetButtonPressed(forAllUsers);
                        }
                        firePropertyChange("buttonPressed", null, null);
                    }
                });
            }
        };
    }

    private void clearColumnFields() {
        columnCaptionField.setText(null);
        columnPatternField.setText(null);
    }

    private void okButtonPressed() {
        for (UserPreferencesPropertyListItem propertyItem : visibleListModel.toArray()) {
            initialTable.setUserColumnSettings(propertyItem.property, propertyItem.getUserCaption(true), propertyItem.getUserPattern(true), visibleListModel.indexOf(propertyItem), false);
        }
        
        String[] hiddenPropSids = new String[invisibleListModel.size()];
        UserPreferencesPropertyListItem[] invisibleItems = invisibleListModel.toArray();
        for (int i = 0; i < invisibleItems.length; i++) {
            UserPreferencesPropertyListItem propertyItem = invisibleItems[i];
            initialTable.setUserColumnSettings(propertyItem.property, propertyItem.getUserCaption(true), propertyItem.getUserPattern(true), visibleListModel.getSize() + i, true);
            if (propertyItem.inGrid == null || propertyItem.inGrid) {
                hiddenPropSids[i] = propertyItem.property.getSID();
            }
        }

        Font tableFont = getInitialFont();
        tableFont = tableFont.deriveFont(getFontStyle(), getFontSize(tableFont.getSize()));
        initialTable.setUserFont(tableFont);
        initialTable.setFont(tableFont);
        
        initialTable.setUserPageSize(getPageSize());

        Integer headerHeight = getHeaderHeight();
        initialTable.setUserHeaderHeight(headerHeight);

        initialTable.setHasUserPreferences(true);
        
        initialTable.updateTable();
        
        initialTable.refreshUPHiddenProps(hiddenPropSids);

        setVisible(false);
        dispose();
    }

    private void applyButtonPressed(boolean forAllUsers) {
        Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> orderDirections = initialTable.getOrderDirections();
        Map<ClientPropertyDraw, Pair<Boolean, Integer>> sortDirections = new HashMap<>();
        int j = 0;
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> entry : orderDirections.entrySet()) {
            sortDirections.put(entry.getKey().first, new Pair<>(entry.getValue(), j));
            j++;
        }

        for (UserPreferencesPropertyListItem propertyItem : visibleListModel.toArray()) {
            applyPropertyUserPreferences(propertyItem, false, visibleListModel.indexOf(propertyItem), sortDirections.get(propertyItem.property));
        }

        for (UserPreferencesPropertyListItem propertyItem : invisibleListModel.toArray()) {
            int propertyOrder = visibleListModel.getSize() + invisibleListModel.indexOf(propertyItem);
            applyPropertyUserPreferences(propertyItem, true, propertyOrder, sortDirections.get(propertyItem.property));
        }

        Font tableFont = getInitialFont();
        tableFont = tableFont.deriveFont(getFontStyle(), getFontSize(tableFont.getSize()));
        initialTable.setUserFont(tableFont);
        initialTable.setUserPageSize(getPageSize());

        Integer headerHeight = getHeaderHeight();
        initialTable.setUserHeaderHeight(headerHeight);
        
        initialTable.saveCurrentPreferences(forAllUsers, saveSuccessCallback, saveFailureCallback);
    }

    private void applyPropertyUserPreferences(UserPreferencesPropertyListItem propertyItem, boolean hide, int propertyOrder,
                                                               Pair<Boolean, Integer> sortDirections) {
        Boolean sortDirection = sortDirections != null ? sortDirections.first : null;
        Integer sortIndex = sortDirections != null ? sortDirections.second : null;
        initialTable.setUserColumnSettings(propertyItem.property, propertyItem.getUserCaption(true), propertyItem.getUserPattern(true), propertyOrder, hide);
        initialTable.setUserSort(propertyItem.property, sortDirection != null ? sortIndex : null);
        initialTable.setUserAscendingSort(propertyItem.property, sortDirection);
    }

    private void resetButtonPressed(boolean forAllUsers) {
        boolean completeReset = false;
        if (forAllUsers) {
            int result = JOptionPane.showConfirmDialog(this, getString("form.grid.preferences.complete.reset"), getString("form.grid.preferences.complete.reset.header"), JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                completeReset = true;
            }
        }
        clearColumnFields();
        initialTable.resetPreferences(forAllUsers, completeReset, saveSuccessCallback, saveFailureCallback);
    }
    
    private Runnable saveSuccessCallback = new Runnable() {
        @Override
        public void run() {
            // после обновления текущих настроек шрифт мог измениться
            FontInfo font = mergeFont();
            refreshValues(font);
            initialTable.setFont(font.deriveFrom(initialTable));
            initialTable.setHeaderHeight(getHeaderHeight());

            initialTable.updateTable();

            preferencesChanged();
        }
    };
    
    private Runnable saveFailureCallback = new Runnable() {
        @Override
        public void run() {
            FontInfo font = mergeFont();
            refreshValues(font);
            initialTable.setFont(font.deriveFrom(initialTable));
        }
    };
    
    private Boolean getPropertyState(ClientPropertyDraw property) {
        if (goController.isPropertyInGrid(property)) {
            return true;
        } else if (goController.isPropertyInPanel(property)) {
            return false;
        }
        return null;
    }
    
    private void refreshValues(FontInfo font) {
        List<ClientPropertyDraw> orderedVisibleProperties = initialTable.getOrderedVisibleProperties(goController.getGroupObjectProperties());
        GridUserPreferences currentPreferences = initialTable.getCurrentPreferences();
        visibleListModel.clear();
        invisibleListModel.clear();
        for (ClientPropertyDraw property : orderedVisibleProperties) {
            visibleListModel.addElement(new UserPreferencesPropertyListItem(property, currentPreferences.getUserCaption(property), currentPreferences.getUserPattern(property), getPropertyState(property)));
        }
        for (ClientPropertyDraw property : goController.getGroupObjectProperties()) {
            if (!orderedVisibleProperties.contains(property)) {
                invisibleListModel.addElement(new UserPreferencesPropertyListItem(property, currentPreferences.getUserCaption(property), currentPreferences.getUserPattern(property), getPropertyState(property)));
            }
        }
        
        fontSizeField.setText(font.getFontSize() + "");
        isFontBoldCheckBox.setSelected(font.bold);
        isFontItalicCheckBox.setSelected(font.italic);

        initialTable.updatePageSizeIfNeeded(false);
        Integer currentPageSize = currentPreferences.getPageSize();
        pageSizeField.setText(currentPageSize == null ? "" : String.valueOf(currentPageSize));

        Integer currentHeaderHeight = currentPreferences.getHeaderHeight();
        headerHeightField.setText(currentHeaderHeight == null ? "" : String.valueOf(currentHeaderHeight));
    }
    
    private FontInfo mergeFont() {
        GridUserPreferences prefs = initialTable.getCurrentPreferences();
        if (prefs.hasUserPreferences()) {
            return prefs.fontInfo;
        } else {
            return FontInfo.createFrom(getInitialFont());
        }
    }

    private Font getInitialFont() {
        FontInfo designFont = initialTable.getDesignFont();
        if (designFont == null) {
            return new Font(initialTable.getFont().getFontName(), Font.PLAIN, FontInfo.DEFAULT_FONT_SIZE);
        }
        return new Font(designFont.fontFamily, designFont.getStyle(), designFont.fontSize);
    }

    private int getFontStyle() {
        return (isFontBoldCheckBox.isSelected() ? Font.BOLD : Font.PLAIN) | (isFontItalicCheckBox.isSelected() ? Font.ITALIC : Font.PLAIN);
    }

    private int getFontSize(int oldSize) {
        try {
            int fontSize = Integer.parseInt(fontSizeField.getText());
            return fontSize != 0 ? fontSize : oldSize;
        } catch (Exception e) {
            return oldSize;
        }
    }

    private Integer getPageSize() {
        try {
            int pageSize = Integer.parseInt(pageSizeField.getText());
            return pageSize != 0 ? pageSize : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getHeaderHeight() {
        try {
            int headerHeight = Integer.parseInt(headerHeightField.getText());
            return headerHeight != 0 ? headerHeight : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getItemPattern(UserPreferencesPropertyListItem item) {
        return BaseUtils.nullEmpty(item.getUserPattern(true));
    }
    
    public abstract void preferencesChanged();
}
