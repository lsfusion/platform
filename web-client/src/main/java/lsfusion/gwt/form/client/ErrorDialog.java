package lsfusion.gwt.form.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.BaseMessages;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.form.ui.dialog.GResizableModalWindow;
import lsfusion.gwt.form.client.form.ui.layout.flex.FlexTabbedPanel;

@SuppressWarnings("GWTStyleCheck")
public class ErrorDialog extends GResizableModalWindow {
    private static final BaseMessages messages = BaseMessages.Instance.get();
    private FlexPanel mainPane;
    private Button closeButton;
    private FlexTabbedPanel stacksPanel = null;
    
    public ErrorDialog(String caption, String message, String javaStack, String lsfStack) {
        super(caption);

        mainPane = new FlexPanel(true);

        Widget messageWidget = new HTML(message);
        messageWidget.addStyleName("errorBox-message");
        mainPane.add(messageWidget, GFlexAlignment.START);
        
        if (javaStack != null || lsfStack != null) {
            stacksPanel = new FlexTabbedPanel();
            if (javaStack != null) {
                TextArea javaTA = new TextArea();
                javaTA.addStyleName("errorBox-stackBox");
                javaTA.setText(javaStack);
                stacksPanel.add(javaTA, "Java");
            }
            if (lsfStack != null) {
                TextArea lsfTA = new TextArea();
                lsfTA.addStyleName("errorBox-stackBox");
                lsfTA.setText(lsfStack);
                stacksPanel.add(lsfTA, "LSF");
            }
            stacksPanel.selectTab(0);
            stacksPanel.setVisible(false);
            
            mainPane.add(stacksPanel, 1, GFlexAlignment.STRETCH, 1, 150);
        }
        
        FlexPanel buttonsPanel = new FlexPanel(false, FlexPanel.Justify.CENTER);

        closeButton = new Button(messages.close(), new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                hide();
            }
        });
        closeButton.addStyleName("errorBox-button");
        buttonsPanel.add(closeButton);

        if (stacksPanel != null) {
            Button moreButton = new Button(messages.more(), new ClickHandler() {
                @Override
                public void onClick(final ClickEvent event) {
                    stacksPanel.setVisible(!stacksPanel.isVisible());
                    int height = 0;
                    Element mainPaneElement = mainPane.getElement();
                    for (int i = 0; i < mainPaneElement.getChildCount(); i++) {
                        height += ((Element) mainPaneElement.getChild(i)).getOffsetHeight();
                    }
                    setContentSize(mainPane.getOffsetWidth(), height);
                }
            });
            moreButton.addStyleName("errorBox-button");
            buttonsPanel.add(moreButton);
        }

        mainPane.add(buttonsPanel, GFlexAlignment.CENTER);

        mainPane.setHeight("100%");
        FocusPanel focusPanel = new FocusPanel(mainPane);
        focusPanel.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                    hide();
                }
            }
        });
        
        setContentWidget(focusPanel);
    }

    public void showCenter() {
        super.center();
        closeButton.setFocus(true);
    }

    public static void show(String caption, String message, String javaStack) {
        show(caption, message, javaStack, null);
    }
    
    public static void show(String caption, String message, String javaStack, String lsfStack) {
        new ErrorDialog(caption, message, javaStack, lsfStack).showCenter();
    }
}
