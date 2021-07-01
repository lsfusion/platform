package lsfusion.client.form.property.cell.classes.controller;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientInputList;
import lsfusion.client.form.property.cell.classes.controller.suggest.BasicComboBoxUI;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.panel.view.CaptureKeyEventsDispatcher;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
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

import static lsfusion.client.base.view.SwingDefaults.getTableCellMargins;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public abstract class TextFieldPropertyEditor extends JFormattedTextField implements PropertyEditor {
    private static final String CANCEL_EDIT_ACTION = "reset-field-edit";

    protected PropertyTableCellEditor tableEditor;
    public AsyncChangeInterface asyncChange;
    public ClientPropertyDraw property;

    String actionSID;
    boolean hasList;
    boolean strict;
    String[] actions;

    EventObject editEvent;

    SuggestBox suggestBox;

    TextFieldPropertyEditor(ClientPropertyDraw property) {
        this(property, null, null);
    }

    TextFieldPropertyEditor(ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        super();
        this.asyncChange = asyncChange;
        this.property = property;

        editEvent = asyncChange != null ? asyncChange.getCurrentEditEvent() : null;

        this.actionSID = asyncChange != null ? asyncChange.getCurrentActionSID() : null;

        ClientInputList inputList = asyncChange != null ? asyncChange.getCurrentInputList() : null;
        this.hasList = inputList != null && !disableSuggest();
        this.strict = inputList != null && inputList.strict;
        this.actions = inputList != null ? inputList.actions : null;

        if (hasList) {
            suggestBox = new SuggestBox((String) value);
            updateAsyncValues(property, "");
        } else {
            setBorder(this);
            setDesign(this);
            setOpaque(true);

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
            asyncChange.getForm().getAsyncValues(property, asyncChange.getColumnKey(0, 0), query, actionSID, result -> suggestBox.updateItems(result.first, strict && !query.isEmpty()));
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
            if(hasList && strict && asyncChange.getContextAction() == null && !suggestBox.isValidValue(suggestBox.getComboBoxEditorText())) {
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

    private void setBorder(JComponent component) {
        Insets insets = getTableCellMargins();
        component.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right - 1));
    }

    private void setDesign(JTextField component) {
        if (property != null) {
            if (property.design != null)
                ClientColorUtils.designComponent(component, property.design);

            Integer valueAlignment = property.getSwingValueAlignment();
            if (valueAlignment != null) {
                component.setHorizontalAlignment(valueAlignment);
            }
        }
    }

    //based on glazedLists AutoCompleteSupport
    class SuggestBox {
        private EventList<Object> items;
        List<String> latestSuggestions = new ArrayList<>();

        private JComboBox comboBox;

        private JTextField comboBoxEditorComponent;

        public SuggestBox(String value) {
            items = GlazedLists.eventListOf();

            comboBox = new JComboBox(new DefaultEventComboBoxModel(items));
            comboBox.setEditable(true);
            setBorder(comboBox);

            comboBox.setUI(new BasicComboBoxUI() {
                @Override
                protected ComboPopup createPopup() {
                    BasicComboPopup popup = new BasicComboPopup(comboBox) {
                        @Override
                        protected void configurePopup() {
                            super.configurePopup();
                            JPanel buttonsPanel = new JPanel();
                            setBackgroundColor(buttonsPanel);

                            JButton refreshButton = new JButton(ClientImages.get("refresh.png"));
                            refreshButton.addActionListener(e -> updateAsyncValues(property, prevQuery));
                            refreshButton.setRequestFocusEnabled(false);
                            buttonsPanel.add(refreshButton);

                            for (int i = 0; i < actions.length; i++) {
                                int index = i;
                                String action = actions[index];
                                JButton button = new JButton(ClientImages.get(action + ".png"));
                                button.addActionListener(e -> {
                                    asyncChange.setContextAction(index);
                                    tableEditor.stopCellEditing();
                                    asyncChange.setContextAction(null);
                                });
                                button.setRequestFocusEnabled(false);
                                buttonsPanel.add(button);
                            }

                            JPanel buttonsTopPanel = new JPanel(new BorderLayout());
                            setBackgroundColor(buttonsTopPanel);
                            buttonsTopPanel.add(buttonsPanel, BorderLayout.EAST);

                            add(buttonsTopPanel);
                            setBackground(SwingDefaults.getTableCellBackground());

                            setOpaque(true);
                        }

                        @Override
                        public void setBorder(Border border) {
                            Insets insets = getTableCellMargins();
                            Border margin = new EmptyBorder(insets.top, insets.left, insets.bottom, insets.right - 1);
                            super.setBorder(new CompoundBorder(border, margin));
                        }

                        //calculate width based on width of elements
                        @Override
                        protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
                            return super.computePopupBounds(px, py, Math.max(comboBox.getPreferredSize().width, pw), ph);
                        }
                    };
                    popup.getAccessibleContext().setAccessibleParent(comboBox);
                    return popup;
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

            addListeners(value);
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
            comboBox.getModel().setSelectedItem(null);
            items.addAll(GlazedLists.eventListOf(result.toArray()));
            latestSuggestions = result;
            comboBox.setMaximumRowCount(result.size());
            //hide and show to call computePopupBounds
            suggestBox.comboBox.hidePopup();
            suggestBox.comboBox.showPopup();
            if (selectFirst) {
                selectPossibleValue(0);
            }
        }

        private void addListeners(String value) {
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
            setDesign(comboBoxEditorComponent);

            //need to catch key events
            CaptureKeyEventsDispatcher.get().setCapture(comboBoxEditorComponent);

            if (!KeyStrokes.isDeleteEvent(editEvent)) {
                setComboBoxEditorText(value);
                comboBoxEditorComponent.selectAll();
            }

            // add a FocusListener to the ComboBoxEditor which selects all text when focus is gained
            comboBoxEditorComponent.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (!KeyStrokes.isSuitableStartFilteringEvent(editEvent)) {
                        comboBoxEditorComponent.selectAll();
                    }
                }
            });

            //catch enter and escape events
            comboBoxEditorComponent.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        //set selected value from dropdown
                        if(comboBox.getSelectedItem() != null) {
                            setComboBoxEditorText((String) comboBox.getSelectedItem());
                        }
                        if (!strict || isValidValue(getComboBoxEditorText())) {
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
