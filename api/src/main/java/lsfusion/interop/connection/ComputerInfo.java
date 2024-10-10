package lsfusion.interop.connection;

import lsfusion.base.BaseUtils;

import java.io.Serializable;
import java.util.Objects;

public class ComputerInfo implements Serializable {

    public final String hostName;
    public final String hostAddress;

    public ComputerInfo(String hostName, String hostAddress) {
        this.hostName = hostName;
        this.hostAddress = hostAddress;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ComputerInfo && Objects.equals(hostName, ((ComputerInfo) o).hostName) && Objects.equals(hostAddress, ((ComputerInfo) o).hostAddress);
    }

    @Override
    public int hashCode() {
        return BaseUtils.nullHash(hostName) * 31 + BaseUtils.nullHash(hostAddress);
    }
}
