package lsfusion.gwt.form.shared.view.actions;

import java.io.IOException;

public class GRunOpenReportAction extends GExecuteAction {
    public boolean openInExcel;

    @SuppressWarnings("UnusedDeclaration")
    public GRunOpenReportAction() {}
    
    public GRunOpenReportAction(boolean openInExcel) {
        this.openInExcel = openInExcel;
    }
    
    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
