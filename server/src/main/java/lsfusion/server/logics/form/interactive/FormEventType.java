package lsfusion.server.logics.form.interactive;

import lsfusion.interop.form.event.FormEvent;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.event.FormServerEvent;

public class FormEventType extends FormServerEvent<FormEventType> {

    public static final FormEventType INIT = new FormEventType();
    public static final FormEventType APPLY = new FormEventType();
    public static final FormEventType BEFOREAPPLY = new FormEventType();
    public static final FormEventType AFTERAPPLY = new FormEventType();
    public static final FormEventType CANCEL = new FormEventType();
    public static final FormEventType OK = new FormEventType();
    public static final FormEventType BEFOREOK = new FormEventType();
    public static final FormEventType AFTEROK = new FormEventType();
    public static final FormEventType CLOSE = new FormEventType();
    public static final FormEventType DROP = new FormEventType();
    public static final FormEventType QUERYCLOSE = new FormEventType();
    public static final FormEventType QUERYOK = new FormEventType();

    @Override
    public FormEventType get(ObjectMapping mapping) {
        return this;
    }
}
