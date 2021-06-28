package lsfusion.client.form.property.cell.classes.controller;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientInputList;
import lsfusion.client.form.property.cell.classes.controller.suggest.BasicComboBoxUI;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.table.view.CellTableInterface;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import static lsfusion.base.ResourceUtils.readImage;
import static lsfusion.client.base.view.SwingDefaults.getTableCellMargins;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public abstract class TextFieldPropertyEditor extends JFormattedTextField implements PropertyEditor {
    private static final String CANCEL_EDIT_ACTION = "reset-field-edit";

    private static ImageIcon refreshIcon = readImage("refresh.png");

    protected PropertyTableCellEditor tableEditor;
    public CellTableInterface table;
    public ClientPropertyDraw property;

    String actionSID;
    boolean hasList;
    boolean strict;
    String[] actions;

    SuggestBox suggestBox;

    TextFieldPropertyEditor(ClientPropertyDraw property) {
        this(property, null);
    }

    TextFieldPropertyEditor(ClientPropertyDraw property, CellTableInterface table) {
        super();
        this.table = table;
        this.property = property;
        this.actionSID = table != null ? table.getCurrentActionSID() : null;

        ClientInputList inputList = table != null ? table.getCurrentInputList() : null;
        this.hasList = inputList != null && !disableSuggest();
        this.strict = inputList != null && inputList.strict;
        this.actions = inputList != null ? inputList.actions : null;

        if (hasList) {
            suggestBox = new SuggestBox();
            updateAsyncValues(property, "");
        } else {
            Insets insets = getTableCellMargins();
            setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right - 1));
            setOpaque(true);

            if (property != null) {
                if (property.design != null) 
                    ClientColorUtils.designComponent(this, property.design);

                Integer valueAlignment = property.getSwingValueAlignment();
                if (valueAlignment != null) {
                    setHorizontalAlignment(valueAlignment);
                }
            }

            addActionListener(e -> tableEditor.stopCellEditing());

            getActionMap().put(CANCEL_EDIT_ACTION, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tableEditor.cancelCellEditing();
                }
            });
        }
    }

    protected boolean disableSuggest() {
        return true;
    }

    String prevQuery = "";
    public void updateAsyncValues(ClientPropertyDraw property, String query) {
        if(!suggestBox.disableUpdate) {
            prevQuery = query;
            table.getForm().getAsyncValues(property, table.getColumnKey(0, 0), query, actionSID, result -> suggestBox.updateItems(result.first, !query.isEmpty()));
        }
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return hasList ? suggestBox.comboBox : this;
    }

    public boolean stopCellEditing() {
        try {
            if(hasList && strict && table.getContextAction() == null && !suggestBox.isValidValue(suggestBox.getComboBoxEditorText())) {
                tableEditor.cancelCellEditing();
            } else {
                commitEdit();
            }
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    public void cancelCellEditing() { }

    @Override
    public String getText() {
        return hasList ? trimToEmpty(suggestBox.getComboBoxEditorText()) : super.getText();
    }

    public Object getCellEditorValue() {
        return this.getValue();
    }

    @Override
    public String toString() {
        if (tableEditor != null) {
            return "TextFieldEditor[" + tableEditor.getTable().getName() + "]: " + super.toString();
        } else {
            return super.toString();
        }
    }

    @Override
    public void replaceSelection(String content){
        if(content.endsWith("\n"))
            content = content.substring(0, content.length()-1);
        try {
            super.replaceSelection(content);
        } catch (IllegalArgumentException e) { // strange java bug, so just suppress that exception
        }
    }

    //based on glazedLists AutoCompleteSupport
    class SuggestBox {
        private EventList<Object> items;
        List<String> latestSuggestions = new ArrayList<>();

        private JComboBox comboBox;

        private JTextField comboBoxEditorComponent;

        public SuggestBox() {
            items = GlazedLists.eventListOf();

            comboBox = new JComboBox<>(new DefaultEventComboBoxModel(items));
            comboBox.setEditable(true);

            comboBox.setUI(new BasicComboBoxUI() {
                @Override
                protected ComboPopup createPopup() {
                    return new BasicComboPopup(comboBox) {
                        @Override
                        protected void configurePopup() {
                            super.configurePopup();
                            JPanel buttonsPanel = new JPanel();
                            setBackgroundColor(buttonsPanel);

                            JButton refreshButton = new JButton(refreshIcon);
                            refreshButton.addActionListener(e -> updateAsyncValues(property, prevQuery));
                            refreshButton.setRequestFocusEnabled(false);
                            buttonsPanel.add(refreshButton);

                            for (int i = 0; i < actions.length; i++) {
                                int index = i;
                                String action = actions[index];
                                JButton button = new JButton(readImage(action + ".png"));
                                button.addActionListener(e -> {
                                    table.setContextAction(index);
                                    tableEditor.stopCellEditing();
                                    table.setContextAction(null);
                                });
                                button.setRequestFocusEnabled(false);
                                buttonsPanel.add(button);
                            }

                            JPanel buttonsTopPanel = new JPanel(new BorderLayout());
                            setBackgroundColor(buttonsTopPanel);
                            buttonsTopPanel.add(buttonsPanel, BorderLayout.EAST);

                            add(buttonsTopPanel);
                            setBackground(SwingDefaults.getTableCellBackground());
                        }
                    };
                }

                private void setBackgroundColor(JPanel panel) {
                    panel.setBackground(SwingDefaults.getTableCellBackground());
                }

                @Override
                protected JButton createArrowButton() {
                    return new JButton() {
                        public int getWidth() {
                            return 0;
                        }
                    };
                }
            });

            addListeners();
        }

        public boolean isValidValue(String value) {
            return latestSuggestions.contains(value);
        }

        private void selectPossibleValue(int index) {
            if(index >= comboBox.getModel().getSize()) {
                index = 0;
            }

            if(comboBox.getModel().getSize() > index) {
                comboBox.setSelectedIndex(index);
            }
        }

        private class MoveAction extends AbstractAction {
            private final int offset;

            public MoveAction(int offset) {
                this.offset = offset;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboBox.isShowing()) {
                    if (comboBox.isPopupVisible()) {
                        selectPossibleValue(comboBox.getSelectedIndex() + offset);
                    } else {
                        comboBox.showPopup();
                    }
                    if(!strict) {
                        setComboBoxEditorText((String) comboBox.getSelectedItem());
                    }
                }
            }
        }

        public String getComboBoxEditorText() {
            return comboBoxEditorComponent.getText();
        }

        public boolean disableUpdate;
        public void setComboBoxEditorText(String value) {
            disableUpdate = true;
            comboBoxEditorComponent.setText(value);
            disableUpdate = false;
        }

        public void updateItems(List<String> result, boolean selectFirst) {
            items.clear();
            items.addAll(GlazedLists.eventListOf(result.toArray()));
            latestSuggestions = result;
            comboBox.setMaximumRowCount(result.size());
            if (selectFirst) {
                selectPossibleValue(0);
            }
            comboBox.showPopup();
        }

        private void addListeners() {
            //stop editing after item selection
            comboBox.setEditor(new BasicComboBoxEditor() {
                @Override
                public void setItem(Object anObject) {
                    super.setItem(anObject);
                    tableEditor.stopCellEditing();
                }
            });

            //cancel editing when popup is canceled
            comboBox.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    tableEditor.cancelCellEditing();
                }
            });

            comboBoxEditorComponent = (JTextField) comboBox.getEditor().getEditorComponent();
            setComboBoxEditorText((String) table.getCurrentEditValue());
            comboBoxEditorComponent.selectAll();

            // add a FocusListener to the ComboBoxEditor which selects all text when focus is gained
            comboBoxEditorComponent.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    comboBoxEditorComponent.selectAll();
                }
            });

            //catch enter and escape events
            comboBoxEditorComponent.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (!strict || isValidValue((String) comboBox.getSelectedItem())) {
                            setComboBoxEditorText((String) comboBox.getSelectedItem());
                            tableEditor.stopCellEditing();
                            e.consume();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        tableEditor.cancelCellEditing();
                        e.consume();
                    }
                }
            });

            //updateAsyncValues on every change
            comboBoxEditorComponent.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    run();
                }
                public void removeUpdate(DocumentEvent e) {
                    run();
                }
                public void insertUpdate(DocumentEvent e) {
                    run();
                }

                public void run() {
                    updateAsyncValues(property, getComboBoxEditorText());
                }
            });

            final ActionMap actionMap = comboBox.getActionMap();

            final Action upAction = new MoveAction(-1);
            final Action downAction = new MoveAction(1);

            // install custom actions for the arrow keys in all non-Apple L&Fs
            actionMap.put("selectPrevious", upAction);
            actionMap.put("selectNext", downAction);
            actionMap.put("selectPrevious2", upAction);
            actionMap.put("selectNext2", downAction);

            // install custom actions for the arrow keys in the Apple Aqua L&F
            actionMap.put("aquaSelectPrevious", upAction);
            actionMap.put("aquaSelectNext", downAction);
        }
    }
}
