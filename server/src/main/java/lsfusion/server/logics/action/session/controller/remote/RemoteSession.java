package lsfusion.server.logics.action.session.controller.remote;

import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.session.SessionInfo;
import lsfusion.interop.session.remote.RemoteSessionInterface;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.navigator.controller.env.ChangesController;
import lsfusion.server.logics.navigator.controller.env.FormController;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.rmi.RemoteException;
import java.sql.SQLException;

public class RemoteSession extends RemoteConnection implements RemoteSessionInterface {

    public RemoteSession(int port, LogicsInstance logicsInstance, AuthenticationToken token, SessionInfo sessionInfo, ExecutionStack stack) throws RemoteException, ClassNotFoundException, SQLException, SQLHandledException, InstantiationException, IllegalAccessException {
        super(port, "session", stack);

        setContext(new RemoteSessionContext(this));
        initContext(logicsInstance, token, sessionInfo, stack);

        dataSession = createSession();
    }

    @Override
    protected void initUser(SecurityManager securityManager, AuthenticationToken token, DataSession session) throws SQLException, SQLHandledException {
        try {
            super.initUser(securityManager, token, session);
        } catch (AuthenticationException e) { // if we have authentication exception, postpone it maybe only noauth will be used (authenticate with anonymous token)
            authException = e;
            super.initUser(securityManager, AuthenticationToken.ANONYMOUS, session);
        }
    }

    @Override
    protected String getCurrentAuthToken() {
        //assert authException == null; // in theory checkEnableApi always should be called first
        return super.getCurrentAuthToken();
    }

    @Override
    protected FormController createFormController() {
        return new FormController() {
            @Override
            public void changeCurrentForm(String form) {
                throw new RuntimeException("not supported");
            }

            @Override
            public String getCurrentForm() {
                return null;
            }
        };
    }

    @Override
    protected ChangesController createChangesController() {
        return new ChangesController() {
            protected DBManager getDbManager() {
                return dbManager;
            }
        };
    }

    @Override
    protected Long getConnectionId() {
        return null;
    }

    @Override
    public Object getProfiledObject() {
        return "rs";
    }

    @Override
    protected void onClose() {
        try {
            dataSession.close();
        } catch (Throwable t) {
            ServerLoggers.sqlSuppLog(t);
        }
        
        super.onClose();
    }
}
