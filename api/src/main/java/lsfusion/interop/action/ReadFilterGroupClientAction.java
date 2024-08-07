package lsfusion.interop.action;

import java.io.IOException;

public class ReadFilterGroupClientAction implements ClientAction {

    public Integer filterGroup;

    public ReadFilterGroupClientAction(Integer filterGroup) {
        this.filterGroup = filterGroup;
    }

    @Override
    public Integer dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}