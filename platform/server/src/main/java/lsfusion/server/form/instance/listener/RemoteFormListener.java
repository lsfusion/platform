package lsfusion.server.form.instance.listener;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.remote.RemoteForm;

public interface RemoteFormListener {
    void formCreated(RemoteForm form);
    void formDestroyed(RemoteForm form);
    boolean currentClassChanged(ConcreteCustomClass customClass);
}
