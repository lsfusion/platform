package platform.server.form.instance.listener;

import platform.server.form.instance.FormInstance;
import platform.server.logics.BusinessLogics;

public interface FocusListener<T extends BusinessLogics<T>> {

    void gainedFocus(FormInstance<T> form);

}
