package lsfusion.utils.utils;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class ListFilesClientAction implements ClientAction {
    private String source;
    private String charset;

    public ListFilesClientAction(String source, String charset) {
        this.source = source;
        this.charset = charset;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        Object result;
        try {
            result =  FileUtils.listFiles(source, charset);
        } catch (Exception e) {
            result = e.getMessage() != null ? e.getMessage() : String.format("Failed to listFiles '%s'", source);
        }
        return result;
    }
}