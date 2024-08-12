package lsfusion.interop.action;

import java.io.IOException;

public class ReadFilterPropertyClientAction implements ClientAction {

    public Integer property;

    public ReadFilterPropertyClientAction(Integer property) {
        this.property = property;
    }

    @Override
    public String dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}