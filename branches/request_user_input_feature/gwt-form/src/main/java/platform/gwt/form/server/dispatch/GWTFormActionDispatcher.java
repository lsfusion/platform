package platform.gwt.form.server.dispatch;

import platform.client.logics.ClientFormChanges;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.view.changes.dto.GFormChangesDTO;
import platform.interop.action.*;
import platform.interop.form.ServerResponse;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.Serializable;

import java.io.IOException;

public class GWTFormActionDispatcher implements ClientActionDispatcher, Serializable {
    private FormSessionObject form;
    public GFormChangesDTO formChanges;

    public GWTFormActionDispatcher(FormSessionObject form) {
        this.form = form;
    }

    public final void dispatchResponse(ServerResponse serverResponse) throws IOException {
        assert serverResponse != null;
        ClientAction[] actions = serverResponse.actions;
        for (ClientAction action : actions) {
            action.dispatch(this);
        }
    }

    @Override
    public void execute(FormClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(DialogClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object execute(RuntimeClientAction action) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(ExportFileClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object execute(ImportFileClientAction action) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object execute(MessageFileClientAction action) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object execute(ChooseClassAction action) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(UserChangedClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(UserReloginClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(MessageClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int execute(ConfirmClientAction action) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(LogMessageClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(OpenFileClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(AudioClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(PrintPreviewClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(RunExcelClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(RunEditReportClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(HideFormClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(ProcessFormChangesClientAction action) {
        try {
            ClientFormChanges changes = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(action.formChanges)), form.clientForm, null);
            formChanges = changes.getGwtFormChangesDTO();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void execute(UpdateCurrentClassClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object execute(RequestUserInputClientAction action) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void execute(EditNotPerformedClientAction action) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
