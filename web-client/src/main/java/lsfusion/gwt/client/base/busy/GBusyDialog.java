package lsfusion.gwt.client.base.busy;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GProgressBar;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.gwt.client.base.view.FormButton.ButtonStyle.SECONDARY;

public class GBusyDialog extends DialogModalWindow {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public Boolean needInterrupt = null;

    ResizableComplexPanel content;
    boolean pauseTopProgressBar = false;
    private final Image topProgressBarDynamic;
    private final Image topProgressBarStatic;

    private final ResizableComplexPanel message;
    private final FormButton btnExit;
    private final FormButton btnReconnect;
    private final FormButton btnCancel;
    private final FormButton btnInterrupt;

    private List prevMessageList;
    private Timer longActionTimer;
    private boolean longAction;

    private int latestWindowHeight;
    private int latestWindowWidth;

    public GBusyDialog() {
        super(false, inDevMode() ? ModalWindowSize.EXTRA_LARGE : ModalWindowSize.LARGE);

        setCaption(messages.busyDialogLoading());

        content = new ResizableComplexPanel();
        content.addStyleName("dialog-busy-content");

        topProgressBarDynamic = new GImage("loading_bar.gif");
        topProgressBarStatic = new GImage("loading_bar.png");
        topProgressBarStatic.setVisible(false);
        topProgressBarDynamic.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                pauseTopProgressBar = true;
                topProgressBarDynamic.setVisible(false);
                topProgressBarStatic.setVisible(true);
            }
        });
        topProgressBarStatic.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                pauseTopProgressBar = false;
                topProgressBarDynamic.setVisible(true);
                topProgressBarStatic.setVisible(false);
            }
        });

        content.add(topProgressBarDynamic);
        content.add(topProgressBarStatic);

        message = new ResizableComplexPanel();
        message.addStyleName("dialog-busy-message-container");
        if (inDevMode())
            message.addStyleName("dialog-busy-message-container-dev-mode");

        content.add(message);

        setBodyWidget(content);

        FormButton btnCopy = new FormButton(messages.busyDialogCopyToClipboard(), SECONDARY);
        if (inDevMode()) {
            btnCopy.addClickHandler(clickEvent -> copyToClipboard());
            addFooterWidget(btnCopy);
        }

        btnExit = new FormButton(messages.busyDialogExit(), SECONDARY, clickEvent -> exitAction());
        btnExit.setEnabled(false);
        addFooterWidget(btnExit);

        btnReconnect = new FormButton(messages.busyDialogReconnect(), SECONDARY, clickEvent -> reconnectAction());
        btnReconnect.setEnabled(false);
        addFooterWidget(btnReconnect);

        btnCancel = new FormButton(messages.cancel(), SECONDARY, clickEvent -> cancelAction());
        btnCancel.setEnabled(false);
        addFooterWidget(btnCancel);

        btnInterrupt = new FormButton(messages.busyDialogBreak(), SECONDARY, clickEvent -> interruptAction());
        btnInterrupt.setEnabled(false);
        addFooterWidget(btnInterrupt);
    }
    
    private static boolean inDevMode() {
        return MainFrame.showDetailedInfo;
    }

    public void scheduleButtonEnabling() {
        longActionTimer = new Timer() {
            @Override
            public void run() {
                btnExit.setEnabled(true);
                btnReconnect.setEnabled(true);
                btnInterrupt.setEnabled(true);
                longAction = true;
            }
        };
        longActionTimer.schedule(inDevMode() ? 5000 : 60000);
    }

    public void hideBusyDialog() {
//        if(isShowing())
        hide();
        message.clear();
        btnExit.setEnabled(false);
        btnReconnect.setEnabled(false);
        btnCancel.setEnabled(false);
        btnInterrupt.setEnabled(false);
        if(longActionTimer != null)
            longActionTimer.cancel();
    }

    public void updateBusyDialog(List messageList) {
        message.clear();

        if (prevMessageList == null)
            prevMessageList = new ArrayList(messageList.size());

        boolean changed = false;
        boolean showTopProgressBar = true;
        boolean visibleCancelBtn = false;
        int progressBarCount = 0;
        LinkedHashMap<String, Boolean> stackLines = new LinkedHashMap<>();
        for (int i = 0; i < messageList.size(); i++) {
            Object prevMessage = prevMessageList.size() > i ? prevMessageList.get(i) : null;
            final Object message = messageList.get(i);
            if (prevMessage == null || !prevMessage.equals(message))
                changed = true;

            if (message instanceof GProgressBar) {
                if (progressBarCount == 0 && stackLines.isEmpty())
                    showTopProgressBar = false;
                if (!stackLines.isEmpty() && inDevMode()) {
                    createStackPanel(stackLines);
                }

                createProgressBarPanel((GProgressBar) message);
                stackLines = new LinkedHashMap<>();
                progressBarCount++;
            } else if (message instanceof Boolean) {
                visibleCancelBtn = true;
            } else if (inDevMode())
                stackLines.put((String) message, changed);

        }

        prevMessageList = messageList;

        if (!stackLines.isEmpty() && inDevMode()) {
            createStackPanel(stackLines);
        }
        if(pauseTopProgressBar)
            topProgressBarStatic.setVisible(showTopProgressBar);
        else
            topProgressBarDynamic.setVisible(showTopProgressBar);

        if (longAction)
            btnCancel.setEnabled(visibleCancelBtn);
    }

    private boolean windowResized() {
        return latestWindowHeight != Window.getClientHeight() || latestWindowWidth != Window.getClientWidth();
    }

    private void copyToClipboard() {
        CopyPasteUtils.putIntoClipboard(message.getElement());
    }

    private void exitAction() {
        //we make logout instead of close browser window because JavaScript can only close windows that were opened by JavaScript
        GwtClientUtils.logout();
    }

    private void reconnectAction() {
        GwtClientUtils.reconnect();
    }

    private void cancelAction() {
        DialogBoxHelper.showConfirmBox(messages.busyDialogCancelTransaction(),
                messages.busyDialogCancelTransactionConfirm(),
                false, new DialogBoxHelper.CloseCallback() {
                    @Override
                    public void closed(DialogBoxHelper.OptionType chosenOption) {
                        if (chosenOption == DialogBoxHelper.OptionType.YES) {
                            needInterrupt = false;
                        }
                    }
                });
    }

    private void interruptAction() {
        DialogBoxHelper.showConfirmBox(messages.busyDialogInterruptTransaction(),
                messages.busyDialogInterruptTransactionConfirm(),
                false, new DialogBoxHelper.CloseCallback() {
                    @Override
                    public void closed(DialogBoxHelper.OptionType chosenOption) {
                        if (chosenOption == DialogBoxHelper.OptionType.YES) {
                            needInterrupt = true;
                        }
                    }
                });
    }

    private void createStackPanel(LinkedHashMap<String, Boolean> stackLines) {
        String messageText = "";
        for (Map.Entry<String, Boolean> stackLine : stackLines.entrySet()) {
            if (stackLine.getValue())
                messageText += "<div style=\"color: var(--focus-color);\">" + stackLine.getKey() + "</div>";
            else
                messageText += (messageText.isEmpty() ? "" : "<br/>") + stackLine.getKey();
        }
        HTML stack = new HTML(messageText);
        stack.addStyleName("dialog-busy-message-item");
        message.add(stack);
    }

    private void createProgressBarPanel(final GProgressBar line) {

        FlexPanel panel = new FlexPanel(true);
        panel.addStyleName("dialog-busy-message-item");
        panel.add(new ProgressBar(0, line.total, line.progress, new ProgressBar.TextFormatter() {
            @Override
            protected String getText(ProgressBar bar, double curProgress) {
                return line.message;
            }
        }), GFlexAlignment.STRETCH);
        if (line.params != null)
            panel.add(new HTML(line.params), GFlexAlignment.STRETCH);

        message.add(panel);
    }

}