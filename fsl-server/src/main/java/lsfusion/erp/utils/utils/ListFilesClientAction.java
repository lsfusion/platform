package lsfusion.erp.utils.utils;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class ListFilesClientAction implements ClientAction {

    String url;

    public ListFilesClientAction(String url) {
        this.url = url;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return getFilesList();
    }

    private Map<String, Boolean> getFilesList() {
        TreeMap<String, Boolean> result = new TreeMap<>();
        if (url != null) {
            File[] filesList = new File(url).listFiles();
            if (filesList != null) {
                for (File file : filesList) {
                    result.put(file.getName(), file.isDirectory());
                }
            }
        }
        return result;
    }
}