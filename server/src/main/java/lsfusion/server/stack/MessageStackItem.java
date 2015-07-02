package lsfusion.server.stack;

import lsfusion.base.col.interfaces.immutable.ImList;

public class MessageStackItem implements ExecutionStackItem {
    private String message;
    
    public MessageStackItem(String message, ImList<String> args) {
        this.message = message;
        if (args.size() > 0) {
            this.message += " : " + args.toString(",");
        }
    }

    @Override
    public String toString() {
        return message;
    }
}
