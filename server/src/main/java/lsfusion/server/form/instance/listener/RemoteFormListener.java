package lsfusion.server.form.instance.listener;

import lsfusion.server.remote.RemoteForm;

public interface RemoteFormListener {
    void formCreated(RemoteForm form);
    void formDestroyed(RemoteForm form);
}
