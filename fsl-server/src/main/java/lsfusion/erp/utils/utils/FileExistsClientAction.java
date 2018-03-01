package lsfusion.erp.utils.utils;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.File;

public class FileExistsClientAction implements ClientAction {
    private String url;

    public FileExistsClientAction(String url) {
        this.url = url;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return url != null && new File(url).exists();
    }

}