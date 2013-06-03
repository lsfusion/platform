package lsfusion.gwt.form.shared.view.actions;

import java.io.IOException;
import java.io.Serializable;

public interface GAction extends Serializable {
    public Object dispatch(GActionDispatcher dispatcher) throws IOException;
}
