package platform.server.form.instance.listener;

import platform.server.classes.ConcreteCustomClass;
import platform.server.remote.RemoteForm;

public interface RemoteFormListener {
    void formCreated(RemoteForm form);
    boolean currentClassChanged(ConcreteCustomClass customClass);
}
