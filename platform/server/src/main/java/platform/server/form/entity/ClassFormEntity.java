package platform.server.form.entity;

import platform.server.form.navigator.NavigatorElement;
import platform.server.logics.BusinessLogics;

public abstract class ClassFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    protected ClassFormEntity(String sID, String caption, boolean iisPrintForm) {
        super(sID, caption, iisPrintForm);
    }

    protected ClassFormEntity(NavigatorElement<T> parent, String sID, String caption) {
        super(parent, sID, caption);
    }

    protected ClassFormEntity(NavigatorElement<T> parent, String sID, String caption, boolean iisPrintForm) {
        super(parent, sID, caption, iisPrintForm);
    }

    protected ClassFormEntity(String sID, String caption) {
        super(sID, caption);
    }

    public abstract ObjectEntity getObject();
}
