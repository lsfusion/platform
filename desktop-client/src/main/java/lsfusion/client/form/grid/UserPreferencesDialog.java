package lsfusion.client.form.grid;

import lsfusion.base.Pair;
import lsfusion.client.ArrayListTransferHandler;
import lsfusion.client.Main;
import lsfusion.client.form.GroupObjectController;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class UserPreferencesDialog extends JDialog {
    
    private GroupObjectController goController;
    private GridTable initialTable;

    private PropertyListModel visibleListModel, invisibleListModel;
    private JList visibleList, invisibleList;
    private JTextField fontSizeField;
    private JTextField pageSizeField;
    private JCheckBox isFontBoldCheckBox;
    private JCheckBox isFontItalicCheckBox;
    private JTextField columnCaptionField;

    public UserPreferencesDialog(Frame owner, final GridTable initialTable, GroupObjectController goController, final boolean canBeSaved) throws IOException {
        super(owner, getString("form.grid.preferences"), true);
        this.initialTable = initialTable;
        this.goController = goController;

        setMinimumSize(new Dimension(500, 500));
        Rectangle bounds = new Rectangle();
        bounds.x = 100;
        bounds.y = 100;
        bounds.width = 500;
        bounds.height = 500;
        setBounds(bounds);
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

        visibleListModel = new PropertyListModel();
        invisibleListModel = new PropertyListModel();
        visibleList = new JList(visibleListModel);
        invisibleList = new JList(invisibleListModel);
        
        visibleList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        visibleList.setTransferHandler(arrayListHandler);
        visibleList.setDragEnabled(true);
        visibleList.setCellRenderer(new ListItemRenderer());
        JScrollPane visibleListView = new JScrollPane(visibleList);
        visibleListView.setPreferredSize(new Dimension(200, 100));
        TitledPanel visiblePanel = new TitledPanel(getString("form.grid.preferences.displayed.columns"));
        visiblePanel.setLayout(new BorderLayout());
        visiblePanel.add(visibleListView, BorderLayout.CENTER);

        visibleList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!visibleList.isFocusable())
                    visibleList.setFocusable(true);
                boolean hasFocus = visibleList.hasFocus() || visibleList.requestFocusInWindow();
                if(hasFocus) {
                    String newText = getSelectedItemCaption(visibleList);
                    if (!columnCaptionField.getText().equals(newText) && visibleList.getSelectedValue() != null) {
                        columnCaptionField.setText(newText);
                    }
                }
            }
        });
       

        visibleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JList list = (JList) e.getSource();
                if (e.getClickCount() == 2) {
                    int index = list.locationToIndex(e.getPoint());
                    invisibleListModel.addElement(visibleListModel.get(index));
                    visibleListModel.remove(index);
                    columnCaptionField.setText(null);
                }
            }
        });
        
        visibleList.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                String caption = getSelectedItemCaption(visibleList);
                if(visibleList.getSelectedValue() != null)
                    invisibleList.clearSelection();
                if(caption != null && !columnCaptionField.getText().equals(caption))
                    columnCaptionField.setText(caption);
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });

        invisibleList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        invisibleList.setTransferHandler(arrayListHandler);
        invisibleList.setDragEnabled(true);
        invisibleList.setCellRenderer(new ListItemRenderer());
        JScrollPane invisibleListView = new JScrollPane(invisibleList);
        invisibleListView.setPreferredSize(new Dimension(200, 100));
        TitledPanel invisiblePanel = new TitledPanel(getString("form.grid.preferences.hidden.columns"));
        invisiblePanel.setLayout(new BorderLayout());
        invisiblePanel.add(invisibleListView, BorderLayout.CENTER);

        invisibleList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!invisibleList.isFocusable())
                    invisibleList.setFocusable(true);
                boolean hasFocus = invisibleList.hasFocus() || invisibleList.requestFocusInWindow();
                if (hasFocus) {
                    String newText = getSelectedItemCaption(invisibleList);
                    if (!columnCaptionField.getText().equals(newText) && (invisibleList.getSelectedValue() != null)) {
                        columnCaptionField.setText(newText);
                    }
                }
            }
        });

        invisibleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JList list = (JList) e.getSource();
                if (e.getClickCount() == 2) {
                    Rectangle r = list.getCellBounds(0, list.getLastVisibleIndex());
                    if (r != null && r.contains(e.getPoint())) {
                        int index = list.locationToIndex(e.getPoint());
                        visibleListModel.addElement(invisibleListModel.get(index));
                        invisibleListModel.remove(index);
                        columnCaptionField.setText(null);
                    }
                }
            }
        });

        invisibleList.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                String caption = getSelectedItemCaption(invisibleList);
                if(invisibleList.getSelectedValue() != null)
                    visibleList.clearSelection();
                if(caption != null && !columnCaptionField.getText().equals(caption))
                    columnCaptionField.setText(caption);
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        
        JButton showAllButton = new JButton(new ImageIcon(Main.class.getResource("/images/arrowLeft.png")));
        showAllButton.setBorder(null);
        showAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < invisibleListModel.getSize(); i++) {
                    visibleListModel.addElement(invisibleListModel.get(i));
                }
                invisibleListModel.clear();
                columnCaptionField.setText(null);
            }
        });

        JButton hideAllButton = new JButton(new ImageIcon(Main.class.getResource("/images/arrowRight.png")));
        hideAllButton.setBorder(null);
        hideAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < visibleListModel.getSize(); i++) {
                    invisibleListModel.addElement(visibleListModel.get(i));
                }
                visibleListModel.clear();
                columnCaptionField.setText(null);
            }
        });

        JPanel arrowsPanel = new JPanel();
        arrowsPanel.setLayout(new BoxLayout(arrowsPanel, BoxLayout.Y_AXIS));
        arrowsPanel.add(hideAllButton);
        arrowsPanel.add(showAllButton);

        JPanel columnsPanel = new JPanel();
        columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.X_AXIS));
        columnsPanel.add(visiblePanel);
        columnsPanel.add(arrowsPanel, BoxLayout.Y_AXIS);
        columnsPanel.add(invisiblePanel);

        
        final JButton applyButton = new JButton(getString("form.grid.preferences.save.settings"));
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    applyButtonPressed(false);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                firePropertyChange("buttonPressed", null, null);
            }
        });

        final JButton resetButton = new JButton(getString("form.grid.preferences.reset.settings"));
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    resetButtonPressed(false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                firePropertyChange("buttonPressed", null, null);
            }
        });

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
                if(invisibleList.getSelectedValue() == null && visibleList.getSelectedValue() != null && !columnCaptionField.getText().isEmpty()) {
                    setSelectedItemCaption(visibleList, columnCaptionField.getText());
                    visibleList.update(visibleList.getGraphics());
                } else if(visibleList.getSelectedValue() == null && invisibleList.getSelectedValue() != null && !columnCaptionField.getText().isEmpty()) {
                    setSelectedItemCaption(invisibleList, columnCaptionField.getText());
                    invisibleList.update(invisibleList.getGraphics());
                }
            }
        });

        TitledPanel columnCaptionPanel = new TitledPanel(getString("form.grid.preferences.selected.column.settings"));
        columnCaptionPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        columnCaptionPanel.add(new JLabel(getString("form.grid.preferences.column.caption") + ": "));
        columnCaptionPanel.add(columnCaptionField);

        pageSizeField = new JTextField(4);
        TitledPanel pageSizePanel = new TitledPanel(getString("form.grid.preferences.page.size.settings"));
        pageSizePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        pageSizePanel.add(new JLabel(getString("form.grid.preferences.page.size") + ": "));
        pageSizePanel.add(pageSizeField);
        
        fontSizeField = new JTextField(2);
        isFontBoldCheckBox = new JCheckBox(getString("descriptor.editor.font.style.bold"));
        isFontItalicCheckBox = new JCheckBox(getString("descriptor.editor.font.style.italic"));
        
        TitledPanel fontPanel = new TitledPanel(getString("form.grid.preferences.font.settings"));
        fontPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        fontPanel.add(new JLabel(getString("descriptor.editor.font.size") + ": "));
        fontPanel.add(fontSizeField);
        fontPanel.add(isFontBoldCheckBox);
        fontPanel.add(isFontItalicCheckBox);

        Box fontAndCaptionSettingsPanel = new Box(BoxLayout.Y_AXIS);
        fontAndCaptionSettingsPanel.add(pageSizePanel);
        fontAndCaptionSettingsPanel.add(columnCaptionPanel);
        fontAndCaptionSettingsPanel.add(fontPanel);
        
        JPanel applyResetButtonsPanel = new JPanel();
        applyResetButtonsPanel.add(currentUserPanel, BorderLayout.WEST);

        if (Main.configurationAccessAllowed) {
            final JButton applyForAllButton = new JButton(getString("form.grid.preferences.save.settings"));
            applyForAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        applyButtonPressed(true);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    firePropertyChange("buttonPressed", null, null);
                }
            });

            final JButton resetForAllButton = new JButton(getString("form.grid.preferences.reset.settings"));
            resetForAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        resetButtonPressed(true);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    firePropertyChange("buttonPressed", null, null);
                }
            });

            TitledPanel allUsersPanelPanel = new TitledPanel(getString("form.grid.preferences.for.all.users"));
            allUsersPanelPanel.add(applyForAllButton, BorderLayout.NORTH);
            allUsersPanelPanel.add(resetForAllButton, BorderLayout.SOUTH);
            applyResetButtonsPanel.add(allUsersPanelPanel, BorderLayout.EAST);
        }

        Box settingsAndApplyResetPanel = new Box(BoxLayout.Y_AXIS);
        settingsAndApplyResetPanel.add(fontAndCaptionSettingsPanel);
        if (canBeSaved) {
            settingsAndApplyResetPanel.add(applyResetButtonsPanel);
        }

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    okButtonPressed();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
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

    private void okButtonPressed() throws IOException {
        for (PropertyListItem propertyItem : visibleListModel.toArray()) {
            initialTable.setUserCaption(propertyItem.property, propertyItem.getUserCaption(true));
            initialTable.setUserOrder(propertyItem.property, visibleListModel.indexOf(propertyItem));
            initialTable.setUserHide(propertyItem.property, false);
        }
        for (PropertyListItem propertyItem : invisibleListModel.toArray()) {
            initialTable.setUserCaption(propertyItem.property, propertyItem.getUserCaption(true));
            initialTable.setUserOrder(propertyItem.property, visibleListModel.getSize() + invisibleListModel.indexOf(propertyItem));
            initialTable.setUserHide(propertyItem.property, true);
        }

        Font tableFont = getInitialFont();
        tableFont = tableFont.deriveFont(getFontStyle(), getFontSize(tableFont.getSize()));
        initialTable.setUserFont(tableFont);
        initialTable.setFont(tableFont);
        
        initialTable.setUserPageSize(getPageSize());
        initialTable.updatePageSizeIfNeeded(false);
        
        initialTable.setHasUserPreferences(true);
        
        initialTable.updateTable();

        setVisible(false);
        dispose();
    }

    private void applyButtonPressed(boolean forAllUsers) throws IOException {
        Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> orderDirections = initialTable.getOrderDirections();
        Map<ClientPropertyDraw, Pair<Boolean, Integer>> sortDirections = new HashMap<ClientPropertyDraw, Pair<Boolean, Integer>>();
        int j = 0;
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> entry : orderDirections.entrySet()) {
            sortDirections.put(entry.getKey().first, new Pair<Boolean, Integer>(entry.getValue(), j));
            j++;
        }

        for (PropertyListItem propertyItem : visibleListModel.toArray()) {
            applyPropertyUserPreferences(propertyItem, false, visibleListModel.indexOf(propertyItem), sortDirections.get(propertyItem.property));
        }

        for (PropertyListItem propertyItem : invisibleListModel.toArray()) {
            int propertyOrder = visibleListModel.getSize() + invisibleListModel.indexOf(propertyItem);
            applyPropertyUserPreferences(propertyItem, true, propertyOrder, sortDirections.get(propertyItem.property));
        }

        Font tableFont = getInitialFont();
        tableFont = tableFont.deriveFont(getFontStyle(), getFontSize(tableFont.getSize()));
        initialTable.setUserFont(tableFont);
        initialTable.setUserPageSize(getPageSize());
        initialTable.updatePageSizeIfNeeded(false);
        
        initialTable.saveCurrentPreferences(forAllUsers, saveSuccessCallback, saveFailureCallback);
    }

    private void applyPropertyUserPreferences(PropertyListItem propertyItem, boolean hide, int propertyOrder,
                                                               Pair<Boolean, Integer> sortDirections) {
        Boolean sortDirection = sortDirections != null ? sortDirections.first : null;
        Integer sortIndex = sortDirections != null ? sortDirections.second : null;
        initialTable.setUserCaption(propertyItem.property, propertyItem.getUserCaption(true));
        initialTable.setUserHide(propertyItem.property, hide);
        initialTable.setUserOrder(propertyItem.property, propertyOrder);
        initialTable.setUserSort(propertyItem.property, sortDirection != null ? sortIndex : null);
        initialTable.setUserAscendingSort(propertyItem.property, sortDirection);
    }

    private void resetButtonPressed(boolean forAllUsers) throws IOException {
        initialTable.resetPreferences(forAllUsers, saveSuccessCallback, saveFailureCallback);
    }
    
    private Runnable saveSuccessCallback = new Runnable() {
        @Override
        public void run() {
            // после обновления текущих настроек шрифт мог измениться
            FontInfo font = mergeFont();
            refreshValues(font);
            initialTable.setFont(font.deriveFrom(initialTable));

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
            visibleListModel.addElement(new PropertyListItem(property, currentPreferences.getUserCaption(property), getPropertyState(property)));
        }
        for (ClientPropertyDraw property : goController.getGroupObjectProperties()) {
            if (!orderedVisibleProperties.contains(property)) {
                invisibleListModel.addElement(new PropertyListItem(property, currentPreferences.getUserCaption(property), getPropertyState(property)));
            }
        }
        
        fontSizeField.setText(font.getFontSize() + "");
        isFontBoldCheckBox.setSelected(font.bold);
        isFontItalicCheckBox.setSelected(font.italic);

        initialTable.updatePageSizeIfNeeded(false); 
        Integer currentPageSize = currentPreferences.getPageSize();
        pageSizeField.setText(currentPageSize == null ? "" : String.valueOf(currentPageSize));
    }
    
    FontInfo mergeFont() {
        GridUserPreferences prefs = initialTable.getCurrentPreferences();
        FontInfo font;
        Font initialFont = getInitialFont();
        if (prefs.hasUserPreferences()) {
            font = prefs.fontInfo;
        } else {
            font = FontInfo.createFrom(initialFont);
        }
        return font;    
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
        int fontSize;
        try {
            fontSize = Integer.parseInt(fontSizeField.getText());
        } catch (Exception e) {
            return oldSize;
        }
        return fontSize != 0 ? fontSize : oldSize;
    }

    private Integer getPageSize() {
        int pageSize;
        try {
            pageSize = Integer.parseInt(pageSizeField.getText());
        } catch (Exception e) {
            return null;
        }
        return pageSize != 0 ? pageSize : null;
    }
    
    private String getSelectedItemCaption(JList list) {
        PropertyListItem item = (PropertyListItem) list.getSelectedValue();
        return item == null ? null : item.getUserCaption(true);
    }
    
    private void setSelectedItemCaption(JList list, String caption) {
        PropertyListItem item = (PropertyListItem) list.getSelectedValue();
        if(item != null && caption != null) {
            initialTable.setUserCaption(item.property, caption);
            item.setUserCaption(caption);
        }
    }
    
    public abstract void preferencesChanged();
  
    private class PropertyListModel extends DefaultListModel  {
        @Override
        public PropertyListItem getElementAt(int index) {
            return (PropertyListItem) super.getElementAt(index);
        }

        @Override
        public PropertyListItem get(int index) {
            return (PropertyListItem) super.get(index);
        }

        @Override
        public PropertyListItem[] toArray() {
            Object[] values = super.toArray();
            return Arrays.copyOf(values, values.length, PropertyListItem[].class);
        }
    }
    
    private class PropertyListItem {
        public ClientPropertyDraw property;
        private String userCaption;
        Boolean inGrid; // false - panel, null - hidden through showIf
        
        public PropertyListItem(ClientPropertyDraw property, String userCaption, Boolean inGrid) {
            this.property = property;
            this.userCaption = userCaption;
            this.inGrid = inGrid;
        }
        
        public String getUserCaption(boolean ignoreDefault) {
            return userCaption != null ? userCaption : (ignoreDefault ? null : property.getCaption());  
        }
        
        public void setUserCaption(String userCaption) {
            this.userCaption = userCaption;
        }

        @Override
        public String toString() {
            String result = getUserCaption(false);
            if (inGrid == null) {
                result += " (" + getString("form.grid.preferences.property.not.shown") + ")";
            } else if (!inGrid) {
                result += " (" + getString("form.grid.preferences.property.in.panel") + ")";
            }
            return result;
        }
    }
    
    private class ListItemRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (((PropertyListItem) value).inGrid == null || !((PropertyListItem) value).inGrid) {
                component.setForeground(Color.LIGHT_GRAY);
            }
            return component;
        }
    }
}
