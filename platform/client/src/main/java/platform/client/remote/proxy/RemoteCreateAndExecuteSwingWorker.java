package platform.client.remote.proxy;

import platform.client.Main;
import platform.client.WaitDialog;
import platform.interop.form.RemoteFormInterface;
import platform.interop.remote.MethodInvocation;
import platform.interop.remote.PendingRemote;

import javax.swing.*;

public class RemoteCreateAndExecuteSwingWorker extends SwingWorker<Object[], Void> {
    PendingRemote remote;
    MethodInvocation[] invocations;
    MethodInvocation creator;

    public RemoteCreateAndExecuteSwingWorker(PendingRemote remote, MethodInvocation creator, MethodInvocation[] invocations) {
        this.remote = remote;
        this.creator = creator;
        this.invocations = invocations;
    }

    @Override
    protected Object[] doInBackground() throws Exception {
        try {
            return remote.createAndExecute(creator, invocations);
        } finally {
            WaitDialog.finish();
        }
    }
}