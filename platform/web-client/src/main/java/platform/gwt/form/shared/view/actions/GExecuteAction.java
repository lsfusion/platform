package platform.gwt.form.shared.view.actions;

import java.io.IOException;

public abstract class GExecuteAction implements GAction {

    public final Object dispatch(GActionDispatcher dispatcher) throws IOException {
        execute(dispatcher);
        return null;
    }

    public abstract void execute(GActionDispatcher dispatcher) throws IOException;
}
