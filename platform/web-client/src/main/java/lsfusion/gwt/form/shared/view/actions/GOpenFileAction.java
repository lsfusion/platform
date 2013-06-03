package lsfusion.gwt.form.shared.view.actions;

import java.io.IOException;

public class GOpenFileAction extends GExecuteAction {
    public String filePath;

    @SuppressWarnings("UnusedDeclaration")
    public GOpenFileAction() {}

    public GOpenFileAction(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
