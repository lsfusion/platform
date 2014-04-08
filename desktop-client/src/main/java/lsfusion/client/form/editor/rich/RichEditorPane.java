package lsfusion.client.form.editor.rich;

import com.google.common.base.Throwables;
import lsfusion.client.form.showtype.ShowTypeView;
import net.atlanticbb.tantlinger.ui.DefaultAction;
import net.atlanticbb.tantlinger.ui.text.CompoundUndoManager;
import net.atlanticbb.tantlinger.ui.text.HTMLUtils;
import net.atlanticbb.tantlinger.ui.text.actions.*;
import org.bushe.swing.action.ActionList;
import org.bushe.swing.action.ActionManager;
import org.bushe.swing.action.ActionUIFactory;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Vector;

import static java.lang.Math.max;
import static java.util.Arrays.asList;

/**
 * based on net.atlanticbb.tantlinger.shef.HTMLEditorPane
 */
public class RichEditorPane extends JPanel {

    private static interface Icons {
        static final ImageIcon cut = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/cut.png"));
        static final ImageIcon copy = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/copy.png"));
        static final ImageIcon paste = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/paste.png"));

        static final ImageIcon undo = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/undo.png"));
        static final ImageIcon redo = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/redo.png"));

        static final ImageIcon bold = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/bold.png"));
        static final ImageIcon italic = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/italic.png"));
        static final ImageIcon underline = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/underline.png"));
        static final ImageIcon strike = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/strike.png"));
        static final ImageIcon subscript = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/subscript.png"));
        static final ImageIcon superscript = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/superscript.png"));
        static final ImageIcon removeFormat = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/removeFormat.png"));

        static final ImageIcon alignLeft = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/alignLeft.png"));
        static final ImageIcon alignCenter = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/alignCenter.png"));
        static final ImageIcon alignRight = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/alignRight.png"));
        static final ImageIcon alignJustify = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/alignJustify.png"));

        static final ImageIcon orderedList = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/orderedList.png"));
        static final ImageIcon unorderedList = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/unorderedList.png"));

        static final ImageIcon hr = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/hr.png"));
        static final ImageIcon image = new ImageIcon(ShowTypeView.class.getResource("/images/richtext/image.png"));
    }
    
    private static final String INVALID_TAGS[] = {"html", "head", "body", "title"};

    private static final Font comboFont = new Font("Dialog", Font.PLAIN, 12);

    private static final String[] fontSizeLabels = new String[] {"default", "xx-small", "x-small", "small", "medium", "large", "x-large", "xx-large"};
    private static final int[] fontSizes = new int[] {-1, 8, 10, 12, 14, 18, 24, 36};
    
    private JEditorPane wysEditor;
    private JComboBox fontCombo;
    private JComboBox fontSizeCombo;
    private JComboBox paragraphCombo;
    private JToolBar toolbar;

    private JPopupMenu wysPopupMenu;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private ActionList allActions;

    private FocusHandler focusHandler = new FocusHandler();
    private FontComboHandler fontComboHandler = new FontComboHandler();
    private ActionComboHandler paragraphComboHandler = new ActionComboHandler();
    private FontSizeComboHandler fontSizeComboHandler = new FontSizeComboHandler();
    private CaretListener caretHandler = new CaretHandler();
    private MouseListener popupHandler = new PopupHandler();

    public RichEditorPane() {
        initUI();
    }
    
    public void setToolbarVisible(boolean visible) {
        toolbar.setVisible(visible);
    }

    @Override
    public boolean requestFocusInWindow() {
        return wysEditor.requestFocusInWindow();
    }

    public JEditorPane getWysEditor() {
        return wysEditor;
    }

    private void initUI() {
        createWysiwygEditor();
        
        createActions();
        
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(wysEditor), BorderLayout.CENTER);
    }

    private void createActions() {
        allActions = new ActionList("editor-actions");

        CutAction cutAction = new CutAction();
        CopyAction copyAction = new CopyAction();
        PasteAction pasteAction = new PasteAction();
        
        ActionList editActions = new ActionList("edit");
        editActions.add(CompoundUndoManager.UNDO);
        editActions.add(CompoundUndoManager.REDO);
        editActions.add(null);
        editActions.add(cutAction);
        editActions.add(copyAction);
        editActions.add(pasteAction);
        editActions.add(null);
        editActions.add(new SelectAllAction());

        allActions.addAll(editActions);

        //create editor popupmenus
        wysPopupMenu = ActionUIFactory.getInstance().createPopupMenu(editActions);

        ActionList fontSizeActions = HTMLEditorActionFactory.createFontSizeActionList();

        allActions.addAll(fontSizeActions);

        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setFocusable(false);

        addActionToToolbar(cutAction, Icons.cut);
        addActionToToolbar(copyAction, Icons.copy);
        addActionToToolbar(pasteAction, Icons.paste);

        toolbar.addSeparator();
        addActionToToolbar(CompoundUndoManager.UNDO, Icons.undo);
        addActionToToolbar(CompoundUndoManager.REDO, Icons.redo);
        
        toolbar.addSeparator();
        addActionToToolbar(new HTMLInlineAction(HTMLInlineAction.BOLD), true, Icons.bold);
        addActionToToolbar(new HTMLInlineAction(HTMLInlineAction.ITALIC), true, Icons.italic);
        addActionToToolbar(new HTMLInlineAction(HTMLInlineAction.UNDERLINE), true, Icons.underline);
        addActionToToolbar(new HTMLInlineAction(HTMLInlineAction.STRIKE), true, Icons.strike);
        addActionToToolbar(new HTMLInlineAction(HTMLInlineAction.SUB), true, Icons.subscript);
        addActionToToolbar(new HTMLInlineAction(HTMLInlineAction.SUP), true, Icons.superscript);
        addActionToToolbar(new ClearStylesAction(), true, Icons.removeFormat);

        toolbar.addSeparator();
        addActionToToolbar(new AlignAction(AlignAction.LEFT), true, Icons.alignLeft);
        addActionToToolbar(new AlignAction(AlignAction.CENTER), true, Icons.alignCenter);
        addActionToToolbar(new AlignAction(AlignAction.RIGHT), true, Icons.alignRight);
        addActionToToolbar(new AlignAction(AlignAction.JUSTIFY), true, Icons.alignJustify);

        toolbar.addSeparator();
        addActionToToolbar(new HTMLBlockAction(HTMLBlockAction.UL), true, Icons.unorderedList);
        addActionToToolbar(new HTMLBlockAction(HTMLBlockAction.OL), true, Icons.orderedList);
        HTMLEditorActionFactory.createListElementActionList();
                
        toolbar.addSeparator();
        addActionToToolbar(new HTMLHorizontalRuleAction(), Icons.hr);
//        addActionToToolbar(new HTMLLinkAction());
//        addActionToToolbar(new SpecialCharAction(););
        addActionToToolbar(new HTMLImageAction(), Icons.image);

//        toolbar.addSeparator();
//        createParagraphCombo();
//        toolbar.add(paragraphCombo);

        toolbar.addSeparator();
        createFontsCombo();
        toolbar.add(fontCombo);

        toolbar.addSeparator();
        createFontSizeCombo();
        toolbar.add(fontSizeCombo);
    }

    @SuppressWarnings("UnusedDeclaration")
    private void createParagraphCombo() {
        ActionList paragraphActions = new ActionList("paragraphActions");
        //HTMLBlockAction юзает ElementWriter, поэтому не будет работать нормально...
        paragraphActions.addAll(HTMLEditorActionFactory.createBlockElementActionList());
        paragraphActions.addAll(HTMLEditorActionFactory.createListElementActionList());

        allActions.addAll(paragraphActions);

        PropertyChangeListener paragraphTypeChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("selected")) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        paragraphCombo.removeActionListener(paragraphComboHandler);
                        paragraphCombo.setSelectedItem(evt.getSource());
                        paragraphCombo.addActionListener(paragraphComboHandler);
                    }
                }
            }
        };
        
        for (Object o : paragraphActions) {
            if (o instanceof DefaultAction) {
                ((DefaultAction) o).addPropertyChangeListener(paragraphTypeChangeListener);
            }
        }

        paragraphCombo = new JComboBox(RichEditorUtils.toActionArray(paragraphActions));
        paragraphCombo.setPreferredSize(new Dimension(120, 22));
        paragraphCombo.setMinimumSize(new Dimension(120, 22));
        paragraphCombo.setMaximumSize(new Dimension(120, 22));
        paragraphCombo.setFont(comboFont);
        paragraphCombo.addActionListener(paragraphComboHandler);
        paragraphCombo.setRenderer(new ActionComboRenderer());
    }

    private void createFontsCombo() {
        Vector fonts = new Vector();
        fonts.add("Default");
        fonts.add("serif");
        fonts.add("sans-serif");
        fonts.add("monospaced");
        fonts.addAll(
                asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
        );

        fontCombo = new JComboBox(fonts);
        fontCombo.setPreferredSize(new Dimension(150, 22));
        fontCombo.setMinimumSize(new Dimension(150, 22));
        fontCombo.setMaximumSize(new Dimension(150, 22));
        fontCombo.setFont(comboFont);
        fontCombo.addActionListener(fontComboHandler);
    }
    
    private void createFontSizeCombo() {
        ActionList fontSizeActions = HTMLEditorActionFactory.createFontSizeActionList();
        allActions.addAll(fontSizeActions);
        
        fontSizeCombo = new JComboBox(fontSizeLabels);
        fontSizeCombo.setPreferredSize(new Dimension(120, 22));
        fontSizeCombo.setMinimumSize(new Dimension(120, 22));
        fontSizeCombo.setMaximumSize(new Dimension(120, 22));
        fontSizeCombo.setFont(comboFont);
        fontSizeCombo.addActionListener(fontSizeComboHandler);
    }

    private void addActionToToolbar(Action action, ImageIcon icon) {
        addActionToToolbar(action, false, icon);
    }
    
    private void addActionToToolbar(Action action, boolean toggle, ImageIcon icon) {
        action.putValue(Action.SMALL_ICON, icon);
        if (toggle) {
            action.putValue(ActionManager.BUTTON_TYPE, ActionManager.BUTTON_TYPE_VALUE_TOGGLE);
        }
        
        AbstractButton button = ActionUIFactory.getInstance().createButton(action);
        button.setText(null);
        button.setMnemonic(0);
        button.setMargin(new Insets(1, 1, 1, 1));
        button.setMaximumSize(new Dimension(22, 22));
        button.setMinimumSize(new Dimension(22, 22));
        button.setPreferredSize(new Dimension(22, 22));
        button.setFocusable(false);
        button.setFocusPainted(false);

        Object name = action.getValue(Action.NAME);
        if (name != null) {
            button.setToolTipText(name.toString());
        }

        Object oKey = action.getValue(Action.ACCELERATOR_KEY);
        if (oKey instanceof KeyStroke) {
            KeyStroke key = (KeyStroke) oKey;
            button.getActionMap().put("acceleratorAction", action);
            button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, "acceleratorAction");
        }

        toolbar.add(button);

        allActions.add(action);
    }

    private void createWysiwygEditor() {
        wysEditor = new JEditorPane();
        wysEditor.setEditorKitForContentType("text/html", new RichEditorKit());
        wysEditor.setContentType("text/html");

        setText(wysEditor, "<div></div>");

        wysEditor.addCaretListener(caretHandler);
        wysEditor.addFocusListener(focusHandler);
        wysEditor.addMouseListener(popupHandler);
        wysEditor.addMouseListener(popupHandler);

        HTMLDocument document = (HTMLDocument) wysEditor.getDocument();
        CompoundUndoManager cuh = new CompoundUndoManager(document, new UndoManager());
        document.addUndoableEditListener(cuh);
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateActions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateActions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateActions();
            }

            private void updateActions() {
                try {
                    allActions.putContextValueForAll(HTMLTextEditAction.EDITOR, wysEditor);
                    allActions.updateEnabledForAll();
                } catch (IllegalArgumentException ignore) {
                    //иногда падает, из-за десинка селекшена... см. javax.swing.text.JTextComponent.getSelectedText()
                }
            }
        });
    }

    public void setText(String text) {
        setText(wysEditor, text);
        
        CompoundUndoManager.discardAllEdits(wysEditor.getDocument());

        updateState();
    }

    public String getText() {
        return removeInvalidTags(wysEditor.getText());
    }
    
    private void updateState() {
        fontCombo.removeActionListener(fontComboHandler);
        String fontName = HTMLUtils.getFontFamily(wysEditor);
        if (fontName == null) {
            fontCombo.setSelectedIndex(0);
        } else {
            fontCombo.setSelectedItem(fontName);
        }
        fontCombo.addActionListener(fontComboHandler);

        fontSizeCombo.removeActionListener(fontSizeComboHandler);
        int sizeIndex = max(0, Arrays.binarySearch(fontSizes, RichEditorUtils.getFontSize(wysEditor)));
        fontSizeCombo.setSelectedIndex(sizeIndex);
        fontSizeCombo.addActionListener(fontSizeComboHandler);

        allActions.putContextValueForAll(HTMLTextEditAction.EDITOR, wysEditor);
        allActions.updateEnabledForAll();
    }

    public static void setText(JEditorPane editorPane, String text) {
        text = removeInvalidTags(text);

        editorPane.setText("");
        try {
            HTMLEditorKit kit = (HTMLEditorKit) editorPane.getEditorKit();
            Document doc = editorPane.getDocument();
            StringReader reader = new StringReader(HTMLUtils.jEditorPaneizeHTML(text));
            kit.read(reader, doc, 0);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public static String removeInvalidTags(String html) {
        for (String invalidTag : INVALID_TAGS) {
            html = html.replaceAll("<" + invalidTag + "[^>]*>", "");
            html = html.replaceAll("</" + invalidTag + ">", "");
        }

        return html.trim();
    }

    private class CaretHandler implements CaretListener {
        public void caretUpdate(CaretEvent e) {
            updateState();
        }
    }

    private class PopupHandler extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            checkForPopupTrigger(e);
        }

        public void mouseReleased(MouseEvent e) {
            checkForPopupTrigger(e);
        }

        private void checkForPopupTrigger(MouseEvent e) {
            if (e.isPopupTrigger() && e.getSource() == wysEditor) {
                wysPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class FocusHandler extends FocusAdapter {
        public void focusGained(FocusEvent e) {
            if (e.getComponent() instanceof JEditorPane) {
                JEditorPane ed = (JEditorPane) e.getComponent();
                CompoundUndoManager.updateUndo(ed.getDocument());
                updateState();
            }
        }
    }

    private class FontComboHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == fontCombo) {
                HTMLDocument document = (HTMLDocument) wysEditor.getDocument();
                CompoundUndoManager.beginCompoundEdit(document);

                RichEditorUtils.setFontFamily(wysEditor, fontCombo.getSelectedIndex() == 0 ? null : fontCombo.getSelectedItem().toString());
                
                CompoundUndoManager.endCompoundEdit(document);
            }
        }
    }
    
    private class FontSizeComboHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource()  == fontSizeCombo) {
                HTMLDocument document = (HTMLDocument) wysEditor.getDocument();
                CompoundUndoManager.beginCompoundEdit(document);

                RichEditorUtils.setFontSize(wysEditor, fontSizeCombo.getSelectedIndex() == 0 ? null : fontSizes[fontSizeCombo.getSelectedIndex()]);
                
                CompoundUndoManager.endCompoundEdit(document);
            }
        }
    }

    private static class ActionComboRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof Action) {
                value = ((Action) value).getValue(Action.NAME);
            }

            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    private static class ActionComboHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JComboBox) {
                JComboBox comboBox = (JComboBox) e.getSource();
                Object selectedItem = comboBox.getSelectedItem();
                if (selectedItem instanceof Action) {
                    ((Action) selectedItem).actionPerformed(e);
                }
            }
        }
    }
}
