package lsfusion.client.form.object.table.grid.user.design.view;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.lambda.Callback;
import lsfusion.client.base.view.ThemedFlatRolloverButton;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.grid.controller.GridController;
import lsfusion.client.form.object.table.grid.user.design.GridUserPreferences;
import lsfusion.client.form.object.table.grid.user.design.UserPreferencesPropertyListItem;
import lsfusion.client.form.object.table.grid.user.toolbar.view.TitledPanel;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.design.FontInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.swing.Box.createHorizontalStrut;
import static javax.swing.Box.createVerticalStrut;
import static lsfusion.client.ClientResourceBundle.getString;

public abstract class UserPreferencesDialog extends JDialog {
    
    private GridController goController;
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

    public UserPreferencesDialog(Frame owner, final GridTable initialTable, GridController goController, final boolean canBeSaved) {
        super(owner, getString("form.grid.preferences"), true);
        this.initialTable = initialTable;
        this.goController = goController;

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
        JPanel arrowsPanel = createArrowsPanel();
        columnsPanel.add(arrowsPanel, BoxLayout.Y_AXIS);
        columnsPanel.add(invisiblePanel);

        columnCaptionField = new JTextField();
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

        columnPatternField = new JTextField();
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

        JPanel columnCaptionPanel = new JPanel();
        columnCaptionPanel.setLayout(new BoxLayout(columnCaptionPanel, BoxLayout.X_AXIS));
        columnCaptionPanel.add(new JLabel(getString("form.grid.preferences.column.caption") + ": "));
        columnCaptionPanel.add(columnCaptionField);
        
        JPanel columnPatternPanel = new JPanel();
        columnPatternPanel.setLayout(new BoxLayout(columnPatternPanel, BoxLayout.X_AXIS));
        columnPatternPanel.add(new JLabel(getString("form.grid.preferences.column.pattern") + ": "));
        columnPatternPanel.add(columnPatternField);
        
        TitledPanel columnSettingsPanel = new TitledPanel(getString("form.grid.preferences.selected.column.settings"));
        columnSettingsPanel.setLayout(new BoxLayout(columnSettingsPanel, BoxLayout.Y_AXIS));
        columnSettingsPanel.add(columnCaptionPanel);
        columnSettingsPanel.add(createVerticalStrut(3));
        columnSettingsPanel.add(columnPatternPanel);

        pageSizeField = new JTextField(4);
        pageSizeField.setMaximumSize(new Dimension(pageSizeField.getPreferredSize().width, pageSizeField.getMaximumSize().height));
        pageSizeField.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel pageSizePanel = new JPanel();
        pageSizePanel.setLayout(new BoxLayout(pageSizePanel, BoxLayout.X_AXIS));
        pageSizePanel.add(new JLabel(getString("form.grid.preferences.page.size") + ": "));
        pageSizePanel.add(pageSizeField);
        pageSizePanel.add(Box.createHorizontalGlue());

        headerHeightField = new JTextField(4);
        headerHeightField.setMaximumSize(new Dimension(headerHeightField.getPreferredSize().width, headerHeightField.getMaximumSize().height));
        headerHeightField.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel headerHeightPanel = new JPanel();
        headerHeightPanel.setLayout(new BoxLayout(headerHeightPanel, BoxLayout.X_AXIS));
        headerHeightPanel.add(new JLabel(getString("form.grid.preferences.header.height") + ": "));
        headerHeightPanel.add(headerHeightField);
        headerHeightPanel.add(Box.createHorizontalGlue());

        fontSizeField = new JTextField(3);
        fontSizeField.setHorizontalAlignment(SwingConstants.RIGHT);
        isFontBoldCheckBox = new JCheckBox(getString("form.grid.preferences.font.style.bold"));
        isFontItalicCheckBox = new JCheckBox(getString("form.grid.preferences.font.style.italic"));
        JPanel fontPanelWrapper = new JPanel();
        fontPanelWrapper.setLayout(new BoxLayout(fontPanelWrapper, BoxLayout.X_AXIS));
        fontPanelWrapper.add(new JLabel(getString("form.grid.preferences.font.size") + ": "));
        fontPanelWrapper.add(fontSizeField);
        fontPanelWrapper.add(createHorizontalStrut(5));
        fontPanelWrapper.add(isFontBoldCheckBox);
        fontPanelWrapper.add(isFontItalicCheckBox);
        TitledPanel fontPanel = new TitledPanel(getString("form.grid.preferences.font"));
        fontPanel.add(fontPanelWrapper, BorderLayout.WEST);

        TitledPanel gridSettingsPanel = new TitledPanel(getString("form.grid.preferences.grid.settings"));
        gridSettingsPanel.setLayout(new BoxLayout(gridSettingsPanel, BoxLayout.Y_AXIS));
        gridSettingsPanel.add(pageSizePanel);
        gridSettingsPanel.add(createVerticalStrut(3));
        gridSettingsPanel.add(headerHeightPanel);
        gridSettingsPanel.add(createVerticalStrut(4));
        gridSettingsPanel.add(fontPanel);
        
        Box gridColumnSettingsPanel = new Box(BoxLayout.Y_AXIS);
        gridColumnSettingsPanel.add(createVerticalStrut(2));
        gridColumnSettingsPanel.add(columnSettingsPanel);
        gridColumnSettingsPanel.add(createVerticalStrut(2));
        gridColumnSettingsPanel.add(gridSettingsPanel);

        Box settingsAndSaveResetPanel = new Box(BoxLayout.Y_AXIS);
        settingsAndSaveResetPanel.add(gridColumnSettingsPanel);
        
        if (canBeSaved) {
            JPanel saveResetButtonsPanel = new JPanel(new BorderLayout());
            final JButton saveButton = new JButton(getString("form.grid.preferences.save"));
            saveButton.addActionListener(createSaveResetButtonListener(true));

            final JButton resetButton = new JButton(getString("form.grid.preferences.reset"));
            resetButton.addActionListener(createSaveResetButtonListener(false));

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new GridLayout(2, 1, 0, 2));
            buttonsPanel.add(saveButton);
            buttonsPanel.add(resetButton);
            
            saveResetButtonsPanel.add(buttonsPanel, BorderLayout.WEST);
            saveResetButtonsPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
            
            settingsAndSaveResetPanel.add(saveResetButtonsPanel);
        }

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButtonPressed();
                firePropertyChange("buttonPressed", null, null);
            }
        });

        JButton cancelButton = new JButton(getString("dialog.cancel"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel okCancelButtons = new JPanel();
        okCancelButtons.add(okButton);
        okCancelButtons.add(cancelButton);

        JPanel settingsOkCancelPanel = new JPanel(new BorderLayout());
        settingsOkCancelPanel.add(settingsAndSaveResetPanel, BorderLayout.NORTH);
        settingsOkCancelPanel.add(okCancelButtons, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(columnsPanel, BorderLayout.CENTER);
        add(settingsOkCancelPanel, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(500, arrowsPanel.getMinimumSize().height + settingsOkCancelPanel.getMinimumSize().height + 70));
        setLocationRelativeTo(owner);
        
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

        JScrollPane listView = new JScrollPane(list);
        list.setCellRenderer(new UserPreferencesListItemRenderer(visible));
        listView.setPreferredSize(new Dimension(200, 100));
        return new TitledPanel(getString(visible ? "form.grid.preferences.displayed.columns" : "form.grid.preferences.hidden.columns"), listView);
    }

    private JPanel createArrowsPanel() {
        JButton showSelectedButton = new ThemedFlatRolloverButton("arrowLeftOne.png");
        showSelectedButton.setBorder(null);
        showSelectedButton.setBackground(null);
        showSelectedButton.addActionListener(createMoveSelectedButtonListener(invisibleList, visibleListModel));

        JButton hideSelectedButton = new ThemedFlatRolloverButton("arrowRightOne.png");
        hideSelectedButton.setBorder(null);
        hideSelectedButton.setBackground(null);
        hideSelectedButton.addActionListener(createMoveSelectedButtonListener(visibleList, invisibleListModel));

        JButton showAllButton = new ThemedFlatRolloverButton("arrowLeft.png");
        showAllButton.setBorder(null);
        showAllButton.setBackground(null);
        showAllButton.addActionListener(createMoveAllButtonListener(invisibleListModel, visibleListModel));

        JButton hideAllButton = new ThemedFlatRolloverButton("arrowRight.png");
        hideAllButton.setBorder(null);
        hideAllButton.setBackground(null);
        hideAllButton.addActionListener(createMoveAllButtonListener(visibleListModel, invisibleListModel));

        JPanel arrowsPanel = new JPanel();
        arrowsPanel.setLayout(new BoxLayout(arrowsPanel, BoxLayout.Y_AXIS));
        arrowsPanel.add(hideSelectedButton);
        arrowsPanel.add(showSelectedButton);
        arrowsPanel.add(createVerticalStrut(10));
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

    private ActionListener createSaveResetButtonListener(final boolean save) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        if (save) {
                            saveButtonPressed();
                        } else {
                            resetButtonPressed();
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
                hiddenPropSids[i] = propertyItem.property.getPropertyFormName();
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
        
        initialTable.update((Boolean) null);
        
        initialTable.refreshUPHiddenProps(hiddenPropSids);

        setVisible(false);
        dispose();
    }

    private void saveButtonPressed() {
        final SaveResetConfirmDialog confirmDialog = new SaveResetConfirmDialog(true);
        confirmDialog.show(new Callback() {
            @Override
            public void done(Object result) {
                Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> orderDirections = initialTable.getOrderDirections();
                Map<ClientPropertyDraw, Pair<Boolean, Integer>> sortDirections = new HashMap<>();
                int j = 0;
                for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> entry : orderDirections.entrySet()) {
                    sortDirections.put(entry.getKey().first, new Pair<>(entry.getValue(), j));
                    j++;
                }

                for (UserPreferencesPropertyListItem propertyItem : visibleListModel.toArray()) {
                    savePropertyUserPreferences(propertyItem, false, visibleListModel.indexOf(propertyItem), sortDirections.get(propertyItem.property));
                }

                for (UserPreferencesPropertyListItem propertyItem : invisibleListModel.toArray()) {
                    int propertyOrder = visibleListModel.getSize() + invisibleListModel.indexOf(propertyItem);
                    savePropertyUserPreferences(propertyItem, true, propertyOrder, sortDirections.get(propertyItem.property));
                }

                Font tableFont = getInitialFont();
                tableFont = tableFont.deriveFont(getFontStyle(), getFontSize(tableFont.getSize()));
                initialTable.setUserFont(tableFont);
                initialTable.setUserPageSize(getPageSize());

                Integer headerHeight = getHeaderHeight();
                initialTable.setUserHeaderHeight(headerHeight);

                initialTable.saveCurrentPreferences(confirmDialog.forAll, saveSuccessCallback, saveFailureCallback);   
            }
        });
    }

    private void savePropertyUserPreferences(UserPreferencesPropertyListItem propertyItem, boolean hide, int propertyOrder,
                                             Pair<Boolean, Integer> sortDirections) {
        Boolean sortDirection = sortDirections != null ? sortDirections.first : null;
        Integer sortIndex = sortDirections != null ? sortDirections.second : null;
        initialTable.setUserColumnSettings(propertyItem.property, propertyItem.getUserCaption(true), propertyItem.getUserPattern(true), propertyOrder, hide);
        initialTable.setUserSort(propertyItem.property, sortDirection != null ? sortIndex : null);
        initialTable.setUserAscendingSort(propertyItem.property, sortDirection);
        initialTable.setHasUserPreferences(true);
    }

    private void resetButtonPressed() {
        final SaveResetConfirmDialog confirmDialog = new SaveResetConfirmDialog(false);
        confirmDialog.show(new Callback() {
            @Override
            public void done(Object result) {
                clearColumnFields();
                initialTable.resetPreferences(confirmDialog.forAll, confirmDialog.complete, saveSuccessCallback, saveFailureCallback);    
            }
        });
    }
    
    private Runnable saveSuccessCallback = new Runnable() {
        @Override
        public void run() {
            // после обновления текущих настроек шрифт мог измениться
            FontInfo font = mergeFont();
            refreshValues(font);
            initialTable.setFont(font.deriveFrom(initialTable));

            initialTable.update((Boolean) null);

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
            int defaultTableFontSize = new JTable().getFont().getSize();
            return new Font(initialTable.getFont().getFontName(), Font.PLAIN, defaultTableFontSize);
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
            return headerHeight >= 0 ? headerHeight : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getItemPattern(UserPreferencesPropertyListItem item) {
        return BaseUtils.nullEmpty(item.getUserPattern(true));
    }
    
    public abstract void preferencesChanged();
}
