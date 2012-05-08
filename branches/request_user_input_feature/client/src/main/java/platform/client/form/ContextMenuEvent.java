package platform.client.form;

import java.util.EventObject;

public class ContextMenuEvent extends EventObject {
    public final String action;
    public ContextMenuEvent(Object source, String action) {
        super(source);
        this.action = action;
    }
}
