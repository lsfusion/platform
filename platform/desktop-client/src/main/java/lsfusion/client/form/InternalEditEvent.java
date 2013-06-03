package lsfusion.client.form;

import java.util.EventObject;

public class InternalEditEvent extends EventObject {
    public final String action;
    public InternalEditEvent(Object source, String action) {
        super(source);
        this.action = action;
    }
}
