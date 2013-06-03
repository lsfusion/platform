package lsfusion.server.form.instance.listener;

import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.BusinessLogics;

public interface FocusListener<T extends BusinessLogics<T>> {

    void gainedFocus(FormInstance<T> form);

}
