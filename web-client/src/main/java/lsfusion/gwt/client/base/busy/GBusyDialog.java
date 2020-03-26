package lsfusion.gwt.client.base.busy;

import com.google.gwt.core.client.ScriptInjector;
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

public class GBusyDialog extends WindowBox {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public Boolean needInterrupt = null;

    VerticalPanel mainPanel;
    VerticalPanel topPanel;
    HorizontalPanel bottomPanel;
    boolean pauseTopProgressBar = false;
    private Image topProgressBarDynamic;
    private Image topProgressBarStatic;

    private ScrollPanel scrollMessagePanel;
    private VerticalPanel messagePanel;
    private Button btnExit;
    private Button btnReconnect;
    private Button btnCancel;
    private Button btnInterrupt;

    private List prevMessageList;
    private Timer longActionTimer;
    private boolean longAction;

    private int latestWindowHeight;
    private int latestWindowWidth;

    @Override
    protected void onLoad() {
        ScriptInjector.fromUrl("static/js/clipboard.js").inject();
    }

    public GBusyDialog() {
        super(false, true, false, false, inDevMode());
        setModal(true);
        setGlassEnabled(true);

        addStyleName("busyDialog");
        setText(messages.busyDialogLoading());

        mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");

        topPanel = new VerticalPanel();
        topPanel.setWidth("100%");

        topPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
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
        topPanel.add(topProgressBarDynamic);
        topPanel.add(topProgressBarStatic);

        topPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        messagePanel = new VerticalPanel();
        messagePanel.setWidth("100%");
        scrollMessagePanel = new ScrollPanel(messagePanel);
        scrollMessagePanel.setHeight("100%");
        topPanel.add(scrollMessagePanel);

        mainPanel.add(topPanel);

        bottomPanel = new HorizontalPanel();
        bottomPanel.setWidth("100%");
        bottomPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(5);

        Button btnCopy = new Button(messages.busyDialogCopyToClipboard());
        if (inDevMode()) {
            btnCopy.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    copyToClipboard();
                }
            });
            buttonPanel.add(btnCopy);
        }

        btnExit = new Button(messages.busyDialogExit());
        btnExit.setEnabled(false);
        btnExit.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                exitAction();
            }
        });
        buttonPanel.add(btnExit);

        btnReconnect = new Button(messages.busyDialogReconnect());
        btnReconnect.setEnabled(false);
        btnReconnect.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                reconnectAction();
            }
        });
        buttonPanel.add(btnReconnect);

        btnCancel = new Button(messages.cancel());
        btnCancel.setEnabled(false);
        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                cancelAction();
            }
        });
        buttonPanel.add(btnCancel);

        btnInterrupt = new Button(messages.busyDialogBreak());
        btnInterrupt.setEnabled(false);
        btnInterrupt.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                interruptAction();
            }
        });
        buttonPanel.add(btnInterrupt);

        bottomPanel.add(buttonPanel);
        mainPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
        mainPanel.add(bottomPanel);

        setWidget(mainPanel);
    }
    
    private static boolean inDevMode() {
        return MainFrame.configurationAccessAllowed;
    }

    public void makeMaskVisible(boolean visible) {
        getElement().getStyle().setOpacity(visible ? 1 : 0);
        getGlassElement().getStyle().setOpacity(visible ? 0.3 : 0);
    }

    public void showBusyDialog() {
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
        hide();
        messagePanel.clear();
        btnExit.setEnabled(false);
        btnReconnect.setEnabled(false);
        btnCancel.setEnabled(false);
        btnInterrupt.setEnabled(false);
        longActionTimer.cancel();
    }

    public void updateBusyDialog(List message) {
            setStackMessageDevMode(message);
    }

    public void setStackMessageDevMode(List messageList) {
        if (prevMessageList == null)
            prevMessageList = new ArrayList(messageList.size());
        boolean changed = false;
        boolean showTopProgressBar = true;
        boolean visibleCancelBtn = false;
        int progressBarCount = 0;
        List<Widget> widgetList = new ArrayList<>();
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
                    widgetList.add(createStackPanel(stackLines));
                }

                widgetList.add(createProgressBarPanel((GProgressBar) message));
                stackLines = new LinkedHashMap<>();
                progressBarCount++;
            } else if (message instanceof Boolean) {
                visibleCancelBtn = true;
            } else if (inDevMode())
                stackLines.put((String) message, changed);

        }

        prevMessageList = messageList;

        if (!stackLines.isEmpty() && inDevMode()) {
            widgetList.add(createStackPanel(stackLines));
        }
        if(pauseTopProgressBar)
            topProgressBarStatic.setVisible(showTopProgressBar);
        else
            topProgressBarDynamic.setVisible(showTopProgressBar);

        messagePanel.clear();
        for (Widget widget : widgetList)
            messagePanel.add(widget);

        if (longAction)
            btnCancel.setEnabled(visibleCancelBtn);


        boolean showMessage = !messageList.isEmpty();
        if (showMessage) {
            boolean resized = windowResized();

            latestWindowWidth = Window.getClientWidth();
            latestWindowHeight = Window.getClientHeight();

            int minWidth = (int) (latestWindowWidth * (inDevMode() ? 0.5 : 0.3));
            int minHeight = (int) (latestWindowHeight * (inDevMode() ? 0.5 : 0.1));

            int width = topPanel.getOffsetWidth() != 0 ? topPanel.getOffsetWidth() : minWidth;
            int topProgressBarHeight = pauseTopProgressBar ?
                    (topProgressBarStatic.isVisible() ? topProgressBarStatic.getElement().getClientHeight() : 0) :
                    (topProgressBarDynamic.isVisible() ? topProgressBarDynamic.getElement().getClientHeight() : 0);
            int height = mainPanel.getElement().getClientHeight() - bottomPanel.getElement().getClientHeight() -
                    topProgressBarHeight - 3; //3 is magic number
            messagePanel.getElement().getStyle().setProperty("maxWidth", width + "px");
            scrollMessagePanel.getElement().getStyle().setProperty("maxWidth", width + "px");
            scrollMessagePanel.getElement().getStyle().setProperty("maxHeight", (height < 0 ? minHeight : height) + "px");

            if (resized) {
                mainPanel.getElement().getStyle().setProperty("minWidth", minWidth + "px");
                getElement().getStyle().setProperty("minWidth", minWidth + "px");

                mainPanel.getElement().getStyle().setProperty("minHeight", minHeight + "px");
                getElement().getStyle().setProperty("minHeight", minHeight + "px");

                if (!inDevMode()) {
                    int maxWidth = (int) (latestWindowWidth * 0.5);
                    mainPanel.getElement().getStyle().setProperty("maxWidth", maxWidth + "px");
                    getElement().getStyle().setProperty("maxWidth", maxWidth + "px");
                }
            }
        }
        if(!isShowing())
            center();
    }

    private boolean windowResized() {
        return latestWindowHeight != Window.getClientHeight() || latestWindowWidth != Window.getClientWidth();
    }

    private void copyToClipboard() {
        String message = "";
        for (int i = 0; i < messagePanel.getWidgetCount(); i++) {
            Widget widget = messagePanel.getWidget(i);
            if (widget instanceof HTML)
                message += ((HTML) widget).getText() + "\n";
        }
        setClipboardData2(message);
    }

    public void setClipboardData2(String text) {



        CopyPasteUtils.setClipboardData2(text);
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

    private HTML createStackPanel(LinkedHashMap<String, Boolean> stackLines) {
        HTML stackPanel = new HTML();
        stackPanel.addStyleName("stackMessage");
        String messageText = "";
        for (Map.Entry<String, Boolean> stackLine : stackLines.entrySet()) {
            if (stackLine.getValue())
                messageText += "<div style=\"color: var(--focus-color);\">" + stackLine.getKey() + "</div>";
            else
                messageText += (messageText.isEmpty() ? "" : "<br/>") + stackLine.getKey();
        }
        stackPanel.setHTML(messageText);
        return stackPanel;
    }

    private FlexPanel createProgressBarPanel(final GProgressBar line) {
        FlexPanel progressBarPanel = new FlexPanel(true);
        progressBarPanel.addStyleName("stackMessage");
        progressBarPanel.add(new ProgressBar(0, line.total, line.progress, new ProgressBar.TextFormatter() {
            @Override
            protected String getText(ProgressBar bar, double curProgress) {
                return line.message;
            }
        }));
        if (line.params != null)
            progressBarPanel.add(new HTML(line.params));
        return progressBarPanel;
    }

}