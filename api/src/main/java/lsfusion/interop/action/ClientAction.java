package lsfusion.interop.action;

import java.io.IOException;
import java.io.Serializable;

public interface ClientAction extends Serializable {
    Object dispatch(ClientActionDispatcher dispatcher) throws IOException;
}
