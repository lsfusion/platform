package platform.client.remote.proxy;

import platform.client.Main;
import platform.client.WaitDialog;
import platform.interop.form.RemoteFormInterface;
import platform.interop.remote.MethodInvocation;
import platform.interop.remote.PendingRemote;

import javax.swing.*;

public class RemoteExecuteSwingWorker extends SwingWorker<Object, Void> {
    PendingRemote remote;
    MethodInvocation[] invocations;

    public RemoteExecuteSwingWorker(PendingRemote remote, MethodInvocation[] invocations) {
        this.remote = remote;
        this.invocations = invocations;
    }

    @Override
    protected Object doInBackground() throws Exception {
        try {
            return remote.execute(invocations);
        } finally {
            WaitDialog.finish();
        }
    }
}