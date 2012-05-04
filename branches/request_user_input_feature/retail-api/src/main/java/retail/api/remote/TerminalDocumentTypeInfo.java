package retail.api.remote;

import java.io.Serializable;

public class TerminalDocumentTypeInfo implements Serializable {
    public String id;
    public String name;
    public String groupName;

    public TerminalDocumentTypeInfo(String id, String name, String groupName) {
        this.id = id;
        this.name = name;
        this.groupName = groupName;
    }
}
