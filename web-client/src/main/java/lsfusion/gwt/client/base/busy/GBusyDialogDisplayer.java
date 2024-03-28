package lsfusion.gwt.client.base.busy;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.ServerMessageProvider;

public class GBusyDialogDisplayer {
    public static final int MESSAGE_UPDATE_PERIOD = 1000;

    private final PopupPanel blockingPanel;
    private final GBusyDialog busyDialog;
    private boolean visible;

    private final Timer showTimer;
    private Widget showPopupOwnerWidget;
    private final Timer hideTimer;

    public GBusyDialogDisplayer(ServerMessageProvider messageProvider) {
        blockingPanel = new BlockingPanel();
        busyDialog = new GBusyDialog();
        showTimer = new Timer() {
            @Override
            public void run() {
                assert visible;
                blockingPanel.hide();
                busyDialog.show(showPopupOwnerWidget);
                showPopupOwnerWidget = null;

                busyDialog.scheduleButtonEnabling();

                Widget popupOwnerWidget = busyDialog.getPopupOwnerWidget();
                updateBusyDialog(messageProvider, popupOwnerWidget); // we want immediate update, to avoid leaps
                Scheduler.get().scheduleFixedPeriod(() -> {
                    if (busyDialog.needInterrupt != null) {
                        messageProvider.interrupt(!busyDialog.needInterrupt, popupOwnerWidget);
                        busyDialog.needInterrupt = null;
                        return true;
                    } else if (visible) {
                        updateBusyDialog(messageProvider, popupOwnerWidget);
                        return true;
                    } else {
                        return false;
                    }
                }, MESSAGE_UPDATE_PERIOD);
            }
        };

        hideTimer = new Timer() {
            @Override
            public void run() {
                stop(true);
            }
        };
    }

    private void updateBusyDialog(ServerMessageProvider messageProvider, Widget popupOwnerWidget) {
        messageProvider.getServerActionMessageList(new PriorityErrorHandlingCallback<ListResult>(popupOwnerWidget) {
            @Override
            public void onSuccess(ListResult result) {
                if (visible) {
                    busyDialog.updateBusyDialog(result.value);
                }
            }
        });
    }

    public boolean isVisible() {
        return visible;
    }

    public void start(Widget popupOwnerWidget) {
        if (!visible) {
            blockingPanel.center();

            showPopupOwnerWidget = popupOwnerWidget;
            showTimer.schedule((int) MainFrame.busyDialogTimeout);
            visible = true;
        }
        hideTimer.cancel();
    }

    public void stop(boolean immediate) {
        if (!visible)
            return;

        if(immediate) {
            blockingPanel.hide();
            busyDialog.hideBusyDialog();

            showTimer.cancel();
            showPopupOwnerWidget = null;

            visible = false;
        } else
            hideTimer.schedule((int) MainFrame.busyDialogTimeout);
    }

    private class BlockingPanel extends PopupPanel {
        public BlockingPanel() {
            setModal(true);
            getElement().getStyle().setOpacity(0);
        }
    }
}