package platform.gwt.view.actions;

import java.io.IOException;

public class GChooseClassAction implements GAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GChooseClassAction() {}

    public final Object dispatch(GActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}
