package platform.gwt.form.shared.view.actions;

import java.io.IOException;
import java.util.ArrayList;

public class GExportFileAction extends GExecuteAction {
    public ArrayList<String> filePaths;

    @SuppressWarnings("UnusedDeclaration")
    public GExportFileAction() {
    }

    public GExportFileAction(ArrayList<String> filePaths) {
        this.filePaths = filePaths;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
