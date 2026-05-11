package lsfusion.client.navigator.controller.remote;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.navigator.LifecycleMessage;
import lsfusion.interop.navigator.PushMessage;

import javax.swing.*;
import java.util.List;

public class ClientCallBackProcessor {

    public ClientCallBackProcessor() {
    }

    public void processMessages(List<LifecycleMessage> messages) {
        if (messages != null) {
            for (LifecycleMessage message : messages) {
                processMessage(message);
            }
        }
    }

    private void processMessage(final LifecycleMessage message) {
        if(message instanceof PushMessage) {
            PushMessage pm = (PushMessage) message;
            final int id = pm.idNotification;
            // Client-side scheduling via Swing Timer. DELAY > 0 waits, then fires; if PERIOD is set
            // a repeating timer is armed after the first fire. Timing is best-effort.
            if (pm.delay > 0L) {
                Timer initial = new Timer((int) pm.delay, e -> {
                    fireOnEdt(id);
                    if (pm.period != null) {
                        new Timer(pm.period.intValue(), e2 -> fireOnEdt(id)).start();
                    }
                });
                initial.setRepeats(false);
                initial.start();
            } else if (pm.period != null) {
                new Timer(pm.period.intValue(), e -> fireOnEdt(id)).start();
            } else {
                fireOnEdt(id);
            }
        }
    }

    private static void fireOnEdt(final int id) {
        SwingUtilities.invokeLater(() -> MainFrame.instance.executeNotificationAction(id));
    }
}
