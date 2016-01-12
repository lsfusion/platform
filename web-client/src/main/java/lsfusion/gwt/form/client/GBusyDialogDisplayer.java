package lsfusion.gwt.form.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.shared.actions.ListResult;
import lsfusion.gwt.form.client.form.ServerMessageProvider;

import java.util.ArrayList;
import java.util.List;

public class GBusyDialogDisplayer extends LoadingManager {
    public static final int GLASS_SCREEN_SHOW_DELAY = 500;
    public static final int MESSAGE_UPDATE_PERIOD = 1000;

    private final GBusyDialog busyDialog;
    private final Timer timer;
    private boolean visible;

    private ServerMessageProvider messageProvider;

    public GBusyDialogDisplayer(ServerMessageProvider messageProvider) {
        this.messageProvider = messageProvider;

        busyDialog = new GBusyDialog();
        timer = new Timer() {
            @Override
            public void run() {
                busyDialog.makeMaskVisible(true);
            }
        };
    }

    public boolean isVisible() {
        return visible;
    }

    public void start() {
        if (!visible) {
            busyDialog.showBusyDialog();
            busyDialog.makeMaskVisible(false);
            timer.schedule(GLASS_SCREEN_SHOW_DELAY);

            if (MainFrame.configurationAccessAllowed) {
                Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
                    @Override
                    public boolean execute() {
                        if (busyDialog.needInterrupt != null) {
                            messageProvider.interrupt(busyDialog.needInterrupt);
                            busyDialog.needInterrupt = null;
                            return true;
                        } else if (visible) {
                            messageProvider.getServerActionMessageList(new ErrorHandlingCallback<ListResult>() {
                                @Override
                                public void success(ListResult result) {
                                    updateBusyDialog(result.value);
                                }
                            });
                            return true;
                        } else {
                            return false;
                        }
                    }
                }, MESSAGE_UPDATE_PERIOD);
            }

            visible = true;
        }
    }

    private void updateBusyDialog(List message) {
        if (visible) {
            busyDialog.updateBusyDialog(message);
        }
    }

    public void stop() {
        if (visible) {
            busyDialog.hideBusyDialog();
            timer.cancel();
            visible = false;
        }
    }
}