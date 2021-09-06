package lsfusion.gwt.client.base.busy;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.ServerMessageProvider;

public class GBusyDialogDisplayer extends LoadingManager {
    public static final int MESSAGE_UPDATE_PERIOD = 1000;

    private final PopupPanel blockingPanel;
    private final GBusyDialog busyDialog;
    private boolean visible;

    private final Timer showTimer;
    private final Timer hideTimer;

    public GBusyDialogDisplayer(ServerMessageProvider messageProvider) {
        blockingPanel = new BlockingPanel();
        busyDialog = new GBusyDialog();
        showTimer = new Timer() {
            @Override
            public void run() {
                assert visible;
                blockingPanel.hide();
                busyDialog.makeMaskVisible(true);

                busyDialog.center();
                busyDialog.scheduleButtonEnabling();

                Scheduler.get().scheduleFixedPeriod(() -> {
                    if (busyDialog.needInterrupt != null) {
                        messageProvider.interrupt(!busyDialog.needInterrupt);
                        busyDialog.needInterrupt = null;
                        return true;
                    } else if (visible) {
                        messageProvider.getServerActionMessageList(new PriorityErrorHandlingCallback<ListResult>() {
                            @Override
                            public void onSuccess(ListResult result) {
                                if (visible) {
                                    busyDialog.updateBusyDialog(result.value);
                                }
                            }
                        });
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

    public boolean isVisible() {
        return visible;
    }

    public void start() {
        if (!visible) {
            blockingPanel.center();
            busyDialog.makeMaskVisible(false);

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