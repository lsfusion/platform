package platform.server.form.instance;

import platform.base.EmptyIterator;
import platform.interop.action.ClientAction;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.remote.RemoteForm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class AutoActionsRunner {
    private final RemoteForm remoteForm;
    private Iterator<PropertyObjectEntity> autoActionsIt;
    private Iterator<ClientAction> actionsIt;
    private FormInstance formInstance;

    public AutoActionsRunner(RemoteForm remoteForm, List<PropertyObjectEntity> autoActions) {
        this.formInstance = remoteForm.form;
        this.remoteForm = remoteForm;
        autoActionsIt = autoActions.iterator();
        actionsIt = new EmptyIterator<ClientAction>();
    }

    private void prepareNext() throws SQLException {
        while (autoActionsIt.hasNext() && !actionsIt.hasNext()) {
            PropertyObjectEntity autoAction = autoActionsIt.next();
            formInstance = remoteForm.form;
            PropertyObjectInstance action = formInstance.instanceFactory.getInstance(autoAction);
            if (action.isInInterface(null)) {
                List<ClientAction> change
                        = formInstance.changeProperty(action,
                                                   formInstance.read(action) == null ? true : null,
                                                   remoteForm, null);
                actionsIt = change.iterator();
            }
        }
    }

    private boolean hasNext() throws SQLException {
        prepareNext();
        return actionsIt.hasNext();
    }

    private ClientAction next() throws SQLException {
        if (hasNext()) {
            return actionsIt.next();
        }
        return null;
    }

    public List<ClientAction> run() throws SQLException {
        List<ClientAction> actions = new ArrayList<ClientAction>();
        while (hasNext()) {
            ClientAction action = next();
            actions.add(action);
//                if (action instanceof ContinueAutoActionsClientAction || action instanceof StopAutoActionsClientAction) {
//                    break;
//                }
        }

        return actions;
    }
}
