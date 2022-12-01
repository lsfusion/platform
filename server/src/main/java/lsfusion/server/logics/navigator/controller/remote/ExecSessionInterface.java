package lsfusion.server.logics.navigator.controller.remote;

import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;


public interface ExecSessionInterface {
    RemoteConnection.ExecSession getExecSession() throws Exception;
}
