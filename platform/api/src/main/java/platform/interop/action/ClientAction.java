package platform.interop.action;

import java.io.IOException;
import java.io.Serializable;

public interface ClientAction extends Serializable {

    public void dispatch(ClientActionDispatcher dispatcher) throws IOException;

    public boolean isBeforeApply();
}
