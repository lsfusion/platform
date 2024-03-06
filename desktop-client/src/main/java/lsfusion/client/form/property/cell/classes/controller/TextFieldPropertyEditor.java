package lsfusion.client.form.property.cell.classes.controller;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;
import lsfusion.base.lambda.AsyncCallback;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientInputList;
import lsfusion.client.form.property.async.ClientInputListAction;
import lsfusion.client.form.property.cell.ClientAsync;
import lsfusion.client.form.property.cell.classes.controller.suggest.BasicComboBoxUI;
import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.panel.view.CaptureKeyEventsDispatcher;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.client.form.property.table.view.AsyncInputComponent;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.property.Compare;
import org.jdesktop.swingx.autocomplete.AutoCompleteComboBoxEditor;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import sun.awt.SunToolkit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

import static lsfusion.client.base.view.SwingDefaults.getTableCellMargins;

public abstract class TextFieldPropertyEditor extends JFormattedTextField implements PropertyEditor {
    private static final String CANCEL_EDIT_ACTION = "reset-field-edit";

    private PropertyTableCellEditor tableEditor;
    private AsyncChangeInterface asyncChange;
    private ClientPropertyDraw property;

    private String actionSID;
    private boolean hasList;
    private CompletionType completionType;
    private ClientInputListAction[] actions;
    private Compare compare;

    private EventObject editEvent;

    private SuggestBox suggestBox;

    private SuggestPopupButton refreshButton;

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
        this.hasList = inputList != null && !disableSuggest() && !property.echoSymbols;
        this.completionType = inputList != null ? inputList.completionType : CompletionType.NON_STRICT;
        this.actions = inputList != null ? inputList.actions : null;
        this.compare = inputList != null ? inputList.compare : null;

        if (hasList) {
            suggestBox = new SuggestBox((String) value);
        } else {
            setBorder(this);
            setDesign(this);
            setOpaque(true);

            // pressed enter, but I have no idea why there is no keycode check or something
            addActionListener(e -> {
                tableEditor.preCommit(true);
                tableEditor.stopCellEditing();
                tableEditor.postCommit();
            });

            // pressed escape default key binding
            getActionMap().put(CANCEL_EDIT_ACTION, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tableEditor.cancelCellEditing();
                }
            });
        }

        getKeymap().setDefaultAction(new DefaultEditorKit.DefaultKeyTypedAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //copy of default method, but allowed 0x1D (GS - group separator character)
                JTextComponent target = getTextComponent(e);
                if ((target != null) && (e != null)) {
                    if ((! target.isEditable()) || (! target.isEnabled())) {
                        return;
                    }
                    String content = e.getActionCommand();
                    int mod = e.getModifiers();
                    if ((content != null) && (!content.isEmpty())) {
                        boolean isPrintableMask = true;
                        Toolkit tk = Toolkit.getDefaultToolkit();
                        if (tk instanceof SunToolkit) {
                            isPrintableMask = ((SunToolkit)tk).isPrintableCharacterModifiersMask(mod);
                        }

                        char c = content.charAt(0);
                        if ((isPrintableMask && (c == 0x1D || (c >= 0x20) && (c != 0x7F))) ||
                                (!isPrintableMask && (c >= 0x200C) && (c <= 0x200D))) {
                            target.replaceSelection(content);
                        }
                    }
                }
            }
        });

        setComponentPopupMenu(new EditorContextMenu(this));

        //hack for scanner input: replace F8 -> GS
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F8 && isScannerEventProperty()) {
                    try {
                        Robot robot = new Robot();

                        robot.keyPress(0x12);//Alt

                        robot.keyPress(0x60);//num 0
                        robot.keyRelease(0x60);

                        robot.keyPress(0x60);//num 0
                        robot.keyRelease(0x60);

                        robot.keyPress(0x62);//num 2
                        robot.keyRelease(0x62);

                        robot.keyPress(0x69);//num 9
                        robot.keyRelease(0x69);

                        robot.keyRelease(0x12);

                        Thread.sleep(100);

                    } catch (AWTException ignore) {
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        //hack for scanner input
        if(isScannerEventProperty()) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isScannerEventProperty() {
        return property != null && property.eventID != null && property.eventID.equals("SCANNER");
    }

    protected boolean disableSuggest() {
        return true;
    }

    private Timer delayTimer;
    private String currentRequest; // current pending request

    private String prevSucceededEmptyQuery;

    private void requestSuggestions() {
        if(!suggestBox.disableUpdate) {
            currentRequest = suggestBox.getComboBoxEditorText();

            if(delayTimer == null)
                updateAsyncValues();
        }
    }
    private void updateAsyncValues() {
        final String query = currentRequest;
        currentRequest = null;

        if(prevSucceededEmptyQuery != null && query.startsWith(prevSucceededEmptyQuery))
            return;


        //disable selection while loading
        suggestBox.clearSelectedIndex();

        suggestBox.updateLoading(true);

        assert delayTimer == null;
        // we're sending a request, so we want to delay all others for at least 100ms
        // also we're using timer to identify the call in cancelAndFlushDelayed
        Timer execTimer = new Timer(100, e -> flushDelayed());
        execTimer.setRepeats(false);
        execTimer.start();
        delayTimer = execTimer;

        JTable table = tableEditor.getTable();
        boolean isEditing = table.isEditing();
        int row = isEditing ? table.getEditingRow() : 0;
        int column = isEditing ? table.getEditingColumn() : 0;
        asyncChange.getForm().getAsyncValues(property, asyncChange.getColumnKey(Math.max(row, 0), Math.max(column, 0)), query, actionSID,
                new AsyncCallback<ClientFormController.ClientAsyncResult>() {
                    @Override
                    public void done(ClientFormController.ClientAsyncResult result) {
                        if (isThisCellEditor()) { // && suggestBox.comboBox.isPopupVisible() it can become visible after callback is completed
                            boolean succeededEmpty = false;
                            if(result.asyncs != null) {
                                suggestBox.updateItems(result.asyncs, (completionType.isStrict() || (completionType.isSemiStrict() && !query.contains(MainController.matchSearchSeparator))) && !query.isEmpty());
                                succeededEmpty = result.asyncs.isEmpty();
                            }

                            suggestBox.updateLoading(result.moreRequests);

                            if (!result.moreRequests && !result.needMoreSymbols) {
                                if (succeededEmpty)
                                    prevSucceededEmptyQuery = query;
                                else
                                    prevSucceededEmptyQuery = null;
                            }
                            
                            cancelAndFlushDelayed(execTimer);
                        }
                    }

                    @Override
                    public void failure(Throwable t) {
                        if (isThisCellEditor()) // suggestBox.comboBox.isPopupVisible()
                            cancelAndFlushDelayed(execTimer);
                    }
                });
    }

    private void cancelAndFlushDelayed(Timer execTimer) {
        if(delayTimer == execTimer) { // we're canceling only if the current timer has not changed
            delayTimer.stop();

            flushDelayed();
        }
    }

    private void flushDelayed() {
        // assert that delaytimer is equal to execTimer
        delayTimer = null;

        if(currentRequest != null) // there was pending request
            updateAsyncValues();
    }

    private void cancelAsyncValues() {
        // this assertion is incorrect in desktop client (unlike in web-client)
//        assert isThisCellEditor();
        if (isThisCellEditor() && suggestBox.isLoading)
            asyncChange.getForm().getAsyncValues(property, asyncChange.getColumnKey(0, 0), null, actionSID, new AsyncCallback<ClientFormController.ClientAsyncResult>() {
                @Override
                public void done(ClientFormController.ClientAsyncResult result) {
                    // assert that CANCELED
                }
                @Override
                public void failure(Throwable t) {
                }
            });
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return hasList ? suggestBox.comboBox : this;
    }

    public boolean stopCellEditing() {
        try {
            if(hasList)
                cancelAsyncValues();
            commitEdit();
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    public void cancelCellEditing() {
        if(hasList)
            cancelAsyncValues();
    }

    @Override
    public String getText() {
        return hasList ? suggestBox.getComboBoxEditorText() : super.getText();
    }

    public Object getCellEditorValue() {
        return this.getValue();
    }
    
    protected boolean isThisCellEditor() {
        boolean isShowing = suggestBox.isShowing();
        // this assertion is incorrect in desktop client (unlike in web-client)
//        JTable currentEditingTable;
//        assert (asyncChange.isEditing() && (currentEditingTable = asyncChange.getForm().getCurrentEditingTable()) != null && currentEditingTable.getCellEditor() == tableEditor) == isShowing;
        return isShowing;
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

    private String initValue;

    //based on glazedLists AutoCompleteSupport
    class SuggestBox {
        private EventList<Object> items;
        List<String> latestSuggestions = new ArrayList<>();

        private CustomComboBox comboBox;

        private JTextField comboBoxEditorComponent;

        public SuggestBox(String value) {
            items = GlazedLists.eventListOf();

            comboBox = new CustomComboBox(new DefaultEventComboBoxModel(items));
            comboBox.setEditable(true);
            
            comboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JComponent comp = (JComponent) super.getListCellRendererComponent(list,value, index, isSelected, cellHasFocus);
                    comp.setBorder(SwingDefaults.getTableCellBorder());
                    return comp;
                }
            });
            setBorder(comboBox);

            comboBox.setUI(new BasicComboBoxUI() {
                @Override
                protected ComboPopup createPopup() {
                    BasicComboPopup popup = new BasicComboPopup(comboBox) {
                        JPanel buttonsTopPanel;
                        
                        @Override
                        protected void configurePopup() {
                            super.configurePopup();
                            JPanel buttonsPanel = new JPanel();
                            setBackgroundColor(buttonsPanel);

                            refreshButton = new SuggestPopupButton(ClientImages.get("refresh.png"), e -> requestSuggestions());
                            buttonsPanel.add(refreshButton);

                            for (ClientInputListAction action : actions) {
                                SuggestPopupButton button = new SuggestPopupButton(ClientImages.get(action.action + ".png"), e -> suggestButtonPressed(action.index));
                                button.setToolTipText(property.getQuickActionTooltipText(action.keyStroke));
                                buttonsPanel.add(button);
                            }

                            buttonsTopPanel = new JPanel(new BorderLayout());
                            setBackgroundColor(buttonsTopPanel);
                            buttonsTopPanel.add(buttonsPanel, BorderLayout.WEST);

                            add(buttonsTopPanel);
                            setBackground(SwingDefaults.getTableCellBackground());

                            setBorder(BorderFactory.createLineBorder(SwingDefaults.getComponentBorderColor()));
                            setOpaque(true);
                        }

                        //calculate width based on width of elements
                        @Override
                        protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
                            //default size + 1px left and right for equal width of textBox and popup
                            Rectangle rectangle = super.computePopupBounds(px - 1, py, Math.max(comboBox.getPreferredSize().width, pw + 2), ph);
                            if (rectangle.y < 0 && buttonsTopPanel != null) {
                                // correcting popup y position on panel with buttons height when popup goes above textfield
                                // may need additional correction if popup would be able to go fullscreen (vertically)
                                rectangle.y -= buttonsTopPanel.getPreferredSize().height;
                            }
                            return rectangle;
                        }
                    };
                    popup.getAccessibleContext().setAccessibleParent(comboBox);
                    return popup;
                }

                private void setBackgroundColor(JPanel panel) {
                    panel.setBackground(SwingDefaults.getPanelBackground());
                }
            });

            addListeners(value);
        }

        private void suggestButtonPressed(Integer index) {
            asyncChange.setContextAction(index);
            tableEditor.stopCellEditing();
            asyncChange.setContextAction(null);
        }
        
        public boolean isShowing() {
            return comboBoxEditorComponent.isShowing();
        }

        public boolean isValidValue(String value) {
            return value.isEmpty() || latestSuggestions.contains(value);
        }

        private void setSelectedIndex(int index) {
            int size = comboBox.getModel().getSize();
            if (index < 0) {
                index = size - 1; //last item
            } else if (index >= size) {
                index = 0; //first item
            }
            if(index < size) {
                comboBox.setSelectedIndex(index);
            }
        }

        private void clearSelectedIndex() {
            comboBox.setSelectedIndex(-1);
        }

        private class MoveAction extends AbstractAction {
            private final int offset;

            public MoveAction(int offset) {
                this.offset = offset;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboBox.isPopupVisible()) {
                    setSelectedIndex(comboBox.getSelectedIndex() + offset);
                }
                if (!completionType.isAnyStrict())
                    updateSelectedEditorText();
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

        private void updateSelectedEditorText() {
            ClientAsync selectedItem = (ClientAsync) comboBox.getSelectedItem();
            if(selectedItem != null)
                setComboBoxEditorText((String) SwingUtils.escapeSeparator(selectedItem.rawString, compare));
        }

        public void updateItems(List<ClientAsync> result, boolean selectFirst) {
            items.clear();
            comboBox.getModel().setSelectedItem(null);
            items.addAll(GlazedLists.eventList(result));
            latestSuggestions = result.stream().map(async -> async.rawString).collect(Collectors.toList());
            comboBox.setMaximumRowCount(result.size());
            //hide and show to call computePopupBounds
            suggestBox.comboBox.hidePopup();
            suggestBox.comboBox.showPopup();
            if (selectFirst) {
                setSelectedIndex(0);
            }
        }

        public boolean isLoading;
        public void updateLoading(boolean isLoading) {
            if(this.isLoading != isLoading) {
                refreshButton.setIcon(ClientImages.get(isLoading ? "loading.gif" : "refresh.png"));
                this.isLoading = isLoading;
            }
        }

        boolean plainPaste = false;
        private void addListeners(String value) {
            //stop editing after item selection
            comboBox.setEditor(new AutoCompleteComboBoxEditor(new BasicComboBoxEditor() {
                @Override
                public void setItem(Object anObject) {
                    super.setItem(SwingUtils.escapeSeparator(anObject, compare));
                    tableEditor.stopCellEditing();
                }
            }, new ObjectToStringConverter() {
                @Override
                public String getPreferredStringForItem(Object item) { // need this, because otherwise combobox editor text will be set to toString (ie formatted text)
                    if(item == null)
                        return null;
                    return ((ClientAsync)item).rawString;
                }
            }));

            //cancel editing when popup is canceled
//            comboBox.addPopupMenuListener(new PopupMenuListener() {
//                @Override
//                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//                }
//
//                @Override
//                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
//                }
//
//                @Override
//                public void popupMenuCanceled(PopupMenuEvent e) {
//                    // canceling / stopping editing on popupMenuCanceled will lead to some odd behaviour when clicking on other editable field
//                    // the problem that stopping editing at this point will schedule focus event on this component, what eventually will lead to cancel editing of the "new" clicked editable Field (in the end it will get focus, but with no editing)
//                    if(strict) {
//                        tableEditor.cancelCellEditing();
//                    } else {
//                        tableEditor.stopCellEditing();
//                    }
//                }
//            });

            comboBoxEditorComponent = (JTextField) comboBox.getEditor().getEditorComponent();
            comboBoxEditorComponent.setDocument(new PlainDocument() {
                @Override
                public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                    if (!plainPaste) {
                        str = modifyInsertString(asyncChange, str);
                    }
                    
                    super.insertString(offs, str, a);
                }
            });
            EditorContextMenu contextMenu = new EditorContextMenu(comboBoxEditorComponent);
            comboBoxEditorComponent.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent mouseEvent) {
                    showContextMenuOnMouseEvent(mouseEvent, contextMenu);
                }

                @Override
                public void mouseReleased(MouseEvent mouseEvent) {
                    showContextMenuOnMouseEvent(mouseEvent, contextMenu);
                }
            });
            setDesign(comboBoxEditorComponent);

            //need to catch key events
            CaptureKeyEventsDispatcher.get().setCapture(comboBoxEditorComponent);

            if (!KeyStrokes.isDeleteEvent(editEvent))
                initValue = value;
            else
                initValue = "";

            // add a FocusListener to the ComboBoxEditor which selects all text when focus is gained
            comboBoxEditorComponent.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (!KeyStrokes.isCharAddKeyEvent(editEvent) && !(editEvent instanceof ActionEvent)) {
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
                        updateSelectedEditorText();
                        if (!completionType.isStrict() || isValidValue(getComboBoxEditorText())) {
                            tableEditor.preCommit(true);
                            tableEditor.stopCellEditing();
                            tableEditor.postCommit();
                        }
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        tableEditor.cancelCellEditing();
                        e.consume();
                    } else if (KeyStrokes.isPlainPasteEvent(e)) {
                        plainPaste = true;
                        comboBoxEditorComponent.paste(); // editor doesn't recognize ctrl+shift+V as paste event by default
                        plainPaste = false;
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_F8/* && isScannerEventProperty()*/) {
                        try {
                            Robot robot = new Robot();
                            robot.keyPress(0x1D);
                            robot.keyRelease(0x1D);
                        } catch (AWTException ignore) {
                        }
                    } else {
                        Integer inputActionIndex = property.getInputActionIndex(e);
                        if(inputActionIndex != null) {
                            suggestButtonPressed(inputActionIndex);
                            e.consume();
                        }
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
                    requestSuggestions();
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

        private void showContextMenuOnMouseEvent(MouseEvent e, JPopupMenu contextMenu) {
            if (e.isPopupTrigger())
                contextMenu.show(comboBoxEditorComponent, e.getX(), e.getY());
        }
    }

    public static String modifyInsertString(AsyncChangeInterface asyncChange, String str) {
        if (str != null && asyncChange != null) {
            try {
                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Object clipboardData = systemClipboard.getData(DataFlavor.stringFlavor);
                
                if (clipboardData != null) {
                    String cpString = clipboardData.toString();
                    if (str.equals(cpString)) { // if equal consider it paste event
                        String newPastedString = (String) asyncChange.modifyPastedString(str);
                        if (newPastedString != null) {
                            str = newPastedString;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return str;
    }

    private class SuggestPopupButton extends JButton {

        public SuggestPopupButton(Icon icon, ActionListener actionListener) {
            super(icon);
            init(actionListener);
        }

        private void init(ActionListener actionListener) {
            setPreferredSize(new Dimension(24, 24));
            setRequestFocusEnabled(false);
            addActionListener(actionListener);
        }
    }


    private class CustomComboBox extends JComboBox implements AsyncInputComponent {

        public CustomComboBox(ComboBoxModel model) {
            super(model);
        }

        @Override
        public void initEditor(EventObject editEvent, boolean selectAll) {
            Integer dialogInputActionIndex = property.getDialogInputActionIndex(actions);
            if (dialogInputActionIndex != null) {
                suggestBox.suggestButtonPressed(dialogInputActionIndex);
                return;
            }

            //need because we extend JComboBox
            suggestBox.comboBoxEditorComponent.putClientProperty("doNotCancelPopup",  BasicComboBoxUI.HIDE_POPUP_KEY());

//            suggestBox.comboBoxEditorComponent.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new HashSet<>());
//            suggestBox.comboBoxEditorComponent.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new HashSet<>());

            //show empty async popup
            // add timer to avoid blinking when empty popup is followed by non-empty one
            Timer showSuggestionsTimer = new Timer(100, e -> {
                if (isThisCellEditor() && !suggestBox.comboBox.isPopupVisible()) {
                    suggestBox.updateItems(Collections.emptyList(), false);
                }
            });
            showSuggestionsTimer.setRepeats(false);
            showSuggestionsTimer.start();

            // don't update suggestions if editing started with char key event. as editor text is empty on init - request is being sent twice
            // wait for editor key listener to catch the event
            if (!KeyStrokes.isCharAddKeyEvent(editEvent)) {
                requestSuggestions();
            }

            suggestBox.setComboBoxEditorText(initValue);
            if (selectAll)
                suggestBox.comboBoxEditorComponent.selectAll();
        }
    }
}
