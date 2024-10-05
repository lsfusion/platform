package lsfusion.interop.connection;

import java.io.Serializable;

public class ConnectionInfo implements Serializable {

    public final ComputerInfo computerInfo;

    public final UserInfo userInfo;

    public ConnectionInfo(ComputerInfo computerInfo, UserInfo userInfo) {
        this.computerInfo = computerInfo;

        this.userInfo = userInfo;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ConnectionInfo && computerInfo.equals(((ConnectionInfo) o).computerInfo) && userInfo.equals(((ConnectionInfo) o).userInfo);
    }

    @Override
    public int hashCode() {
        return 31 * computerInfo.hashCode() + userInfo.hashCode();
    }
}
