package lsfusion.gwt.form.shared.view.grid.editor.rich;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.InitializeEvent;
import com.google.gwt.event.logical.shared.InitializeHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.impl.RichTextAreaImpl;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.FlexPanel;

public class RichTextAreaToolbar extends Composite {
    private Images images = GWT.create(Images.class);
    private Strings strings = GWT.create(Strings.class);
    private EventHandler handler = new EventHandler();

    private RichTextArea textArea;
    private RichTextArea.Formatter formatter;
    
    private boolean textAreaReady = false;

    private FlexPanel toolbarPanel = new FlexPanel(false, FlexPanel.Justify.LEADING);
    
    private PushButton undo;
    private PushButton redo;
    private ToggleButton bold;
    private ToggleButton italic;
    private ToggleButton underline;
    private ToggleButton subscript;
    private ToggleButton superscript;
    private ToggleButton strike;
    private PushButton alignLeft;
    private PushButton alignCenter;
    private PushButton alignRight;
    private PushButton alignJustify;
    private PushButton hr;
    private PushButton ol;
    private PushButton ul;
    private PushButton insertImage;
//    private PushButton createLink;
//    private PushButton removeLink;
    private PushButton removeFormat;

    private ListBox fonts;
    private ListBox fontSizes;

    public RichTextAreaToolbar(RichTextArea textArea) {
        this.textArea = textArea;
        this.formatter = textArea.getFormatter();

        initWidget(toolbarPanel);

        setStyleName("gwt-RichTextToolbar");

        initLayout();

        textArea.addKeyUpHandler(handler);
        textArea.addClickHandler(handler);
        textArea.addInitializeHandler(new InitializeHandler() {
            @Override
            public void onInitialize(InitializeEvent event) {
                textAreaReady = true;
            }
        });
    }

    public void initLayout() {
        toolbarPanel.addCentered(undo = createPushButton(images.undo(), strings.undo()));
        toolbarPanel.addCentered(redo = createPushButton(images.redo(), strings.redo()));

        addSeparator();

        toolbarPanel.addCentered(bold = createToggleButton(images.bold(), strings.bold()));
        toolbarPanel.addCentered(italic = createToggleButton(images.italic(), strings.italic()));
        toolbarPanel.addCentered(underline = createToggleButton(images.underline(), strings.underline()));
        toolbarPanel.addCentered(strike = createToggleButton(images.strike(), strings.strikeThrough()));
        toolbarPanel.addCentered(subscript = createToggleButton(images.subscript(), strings.subscript()));
        toolbarPanel.addCentered(superscript = createToggleButton(images.superscript(), strings.superscript()));
        toolbarPanel.addCentered(removeFormat = createPushButton(images.removeFormat(), strings.removeFormat()));
        
        addSeparator();

        toolbarPanel.addCentered(alignLeft = createPushButton(images.alignLeft(), strings.alignLeft()));
        toolbarPanel.addCentered(alignCenter = createPushButton(images.alignCenter(), strings.alignCenter()));
        toolbarPanel.addCentered(alignRight = createPushButton(images.alignRight(), strings.alignRight()));
        toolbarPanel.addCentered(alignJustify = createPushButton(images.alignJustify(), strings.alignJustify()));

        addSeparator();

        toolbarPanel.addCentered(ol = createPushButton(images.orderedList(), strings.ol()));
        toolbarPanel.addCentered(ul = createPushButton(images.unorderedList(), strings.ul()));

        addSeparator();

//        toolbarPanel.addCentered(createLink = createPushButton(images.createLink(), strings.createLink()));
//        toolbarPanel.addCentered(removeLink = createPushButton(images.removeLink(), strings.removeLink()));
        toolbarPanel.addCentered(hr = createPushButton(images.hr(), strings.hr()));
        toolbarPanel.addCentered(insertImage = createPushButton(images.image(), strings.insertImage()));

        addSeparator();

        toolbarPanel.addCentered(fonts = createFontList());
        toolbarPanel.addCentered(fontSizes = createFontSizes());
    }

    private void addSeparator() {
        toolbarPanel.addCentered(GwtClientUtils.createHorizontalStrut(7));
    }

    private ListBox createFontList() {
        ListBox lb = new ListBox();
        lb.addChangeHandler(handler);
        lb.setVisibleItemCount(1);

        lb.addItem(strings.font(), "");
        lb.addItem(strings.normal(), "");
        lb.addItem("Times New Roman", "Times New Roman");
        lb.addItem("Arial", "Arial");
        lb.addItem("Courier New", "Courier New");
        lb.addItem("Georgia", "Georgia");
        lb.addItem("Trebuchet", "Trebuchet");
        lb.addItem("Verdana", "Verdana");
        return lb;
    }

    private ListBox createFontSizes() {
        ListBox lb = new ListBox();
        lb.addChangeHandler(handler);
        lb.setVisibleItemCount(1);

        lb.addItem(strings.size());
        lb.addItem(strings.xxsmall());
        lb.addItem(strings.xsmall());
        lb.addItem(strings.small());
        lb.addItem(strings.medium());
        lb.addItem(strings.large());
        lb.addItem(strings.xlarge());
        lb.addItem(strings.xxlarge());
        return lb;
    }

    private PushButton createPushButton(ImageResource img, String tip) {
        PushButton pb = new PushButton(new Image(img));
        pb.addClickHandler(handler);
        pb.setTitle(tip);
        return pb;
    }

    private ToggleButton createToggleButton(ImageResource img, String tip) {
        ToggleButton tb = new ToggleButton(new Image(img));
        tb.addClickHandler(handler);
        tb.setTitle(tip);
        return tb;
    }

    private void updateStatus() {
        undo.setEnabled(isUndoable());
        redo.setEnabled(isRedoable());
        bold.setDown(formatter.isBold());
        italic.setDown(formatter.isItalic());
        underline.setDown(formatter.isUnderlined());
        subscript.setDown(formatter.isSubscript());
        superscript.setDown(formatter.isSuperscript());
        strike.setDown(formatter.isStrikethrough());
    }

    private boolean isUndoable() {
        return queryCommandState("Undo");
    }
    
    private boolean isRedoable() {
        return queryCommandState("Redo");
    }

    private boolean queryCommandState(String cmd) {
        if (textAreaReady && formatter instanceof RichTextAreaImpl) {
            // When executing a command, focus the iframe first, since some commands don't take properly when it's not focused.
            textArea.setFocus(true);
            try {
                return queryCommandStateAssumingFocus(((RichTextAreaImpl) formatter).getElement(), cmd);
            } catch (JavaScriptException e) {
                return true;
            }
        }
        return false;
    }

    void execCommand(String cmd, Object param) {
        if (textAreaReady && formatter instanceof RichTextAreaImpl) {
            // When executing a command, focus the iframe first, since some commands
            // don't take properly when it's not focused.
            textArea.setFocus(true);
            try {
                execCommandAssumingFocus(((RichTextAreaImpl) formatter).getElement(), cmd, param);
            } catch (JavaScriptException e) {
                // In mozilla, editing throws a JS exception if the iframe is *hidden, but attached*.
            }
        }
    }

    native void execCommandAssumingFocus(Element element, String cmd, Object param) /*-{
        elem.contentWindow.document.execCommand(cmd, false, param);
    }-*/;

    native boolean queryCommandStateAssumingFocus(Element element, String cmd) /*-{
        return !!elem.contentWindow.document.queryCommandEnabled(cmd);
    }-*/;


    public interface Images extends ClientBundle {
        @Source("images/redo.png")
        ImageResource redo();

        @Source("images/undo.png")
        ImageResource undo();

        @Source("images/bold.png")
        ImageResource bold();

        @Source("images/italic.png")
        ImageResource italic();

        @Source("images/underline.png")
        ImageResource underline();

        @Source("images/strike.png")
        ImageResource strike();

        @Source("images/subscript.png")
        ImageResource subscript();

        @Source("images/superscript.png")
        ImageResource superscript();

        @Source("images/removeFormat.png")
        ImageResource removeFormat();

        @Source("images/alignLeft.png")
        ImageResource alignLeft();

        @Source("images/alignCenter.png")
        ImageResource alignCenter();

        @Source("images/alignRight.png")
        ImageResource alignRight();

        @Source("images/alignJustify.png")
        ImageResource alignJustify();

        @Source("images/orderedList.png")
        ImageResource orderedList();

        @Source("images/unorderedList.png")
        ImageResource unorderedList();

        @Source("images/createLink.png")
        ImageResource createLink();

        @Source("images/removeLink.png")
        ImageResource removeLink();

        @Source("images/hr.png")
        ImageResource hr();

        @Source("images/image.png")
        ImageResource image();
    }

    public interface Strings extends Constants {
        String undo();
        
        String redo();
        
        String bold();

        String italic();

        String underline();

        String strikeThrough();

        String subscript();

        String superscript();

        String removeFormat();

        String alignLeft();

        String alignCenter();

        String alignRight();

        String alignJustify();

        String ul();

        String ol();

        String removeLink();

        String createLink();

        String hr();

        String insertImage();

        String font();

        String normal();

        String size();

        String xxsmall();

        String xsmall();

        String small();

        String medium();

        String large();

        String xlarge();

        String xxlarge();

        String enterUrlPrompt();
    }

    private class EventHandler implements ClickHandler, ChangeHandler, KeyUpHandler {

        public void onChange(ChangeEvent event) {
            Widget sender = (Widget) event.getSource();

            if (sender == fonts) {
                int selectedIndex = fonts.getSelectedIndex();
                if (selectedIndex > 0) {
                    formatter.setFontName(fonts.getValue(selectedIndex));
                    fonts.setSelectedIndex(0);
                }
            } else if (sender == fontSizes) {
                int selectedIndex = fontSizes.getSelectedIndex();
                if (selectedIndex > 0) {
                    execCommand("fontSize", selectedIndex);
                    fontSizes.setSelectedIndex(0);
                }
            }
        }

        public void onClick(ClickEvent event) {
            Widget sender = (Widget) event.getSource();

            if (sender == undo) {
                formatter.undo();
            } else if (sender == redo) {
                formatter.redo();
            } else if (sender == bold) {
                formatter.toggleBold();
            } else if (sender == italic) {
                formatter.toggleItalic();
            } else if (sender == underline) {
                formatter.toggleUnderline();
            } else if (sender == subscript) {
                formatter.toggleSubscript();
            } else if (sender == superscript) {
                formatter.toggleSuperscript();
            } else if (sender == strike) {
                formatter.toggleStrikethrough();
            } else if (sender == alignLeft) {
                formatter.setJustification(RichTextArea.Justification.LEFT);
            } else if (sender == alignCenter) {
                formatter.setJustification(RichTextArea.Justification.CENTER);
            } else if (sender == alignRight) {
                formatter.setJustification(RichTextArea.Justification.RIGHT);
            } else if (sender == alignJustify) {
                formatter.setJustification(RichTextArea.Justification.FULL);
            } else if (sender == insertImage) {
                String url = Window.prompt(strings.enterUrlPrompt(), "http://");
                if (url != null) {
                    formatter.insertImage(url);
                }
//            } else if (sender == createLink) {
//                String url = Window.prompt("Enter a link URL:", "http://");
//                if (url != null) {
//                    formatter.createLink(url);
//                }
//            } else if (sender == removeLink) {
//                formatter.removeLink();
            } else if (sender == hr) {
                formatter.insertHorizontalRule();
            } else if (sender == ol) {
                formatter.insertOrderedList();
            } else if (sender == ul) {
                formatter.insertUnorderedList();
            } else if (sender == removeFormat) {
                formatter.removeFormat();
            } else if (sender == textArea) {
                updateStatus();
            }
            updateStatus();
        }

        public void onKeyUp(KeyUpEvent event) {
            Widget sender = (Widget) event.getSource();
            if (sender == textArea) {
                updateStatus();
            }
        }
    }
}