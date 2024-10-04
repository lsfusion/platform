package lsfusion.interop.connection;

import java.io.Serializable;

public class ConnectionInfo implements Serializable {

    public final ComputerInfo computerInfo;

    public final UserInfo userInfo;

    public ConnectionInfo(ComputerInfo computerInfo, UserInfo userInfo) {
        this.computerInfo = computerInfo;

        this.userInfo = userInfo;
    }
}
