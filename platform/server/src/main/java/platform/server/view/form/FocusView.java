package platform.server.view.form;

import platform.server.logics.BusinessLogics;

public interface FocusView<T extends BusinessLogics<T>> {

    void gainedFocus(RemoteForm<T> form);

}
