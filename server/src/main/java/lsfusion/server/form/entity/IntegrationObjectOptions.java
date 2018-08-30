package lsfusion.server.form.entity;

import java.util.List;

public class IntegrationObjectOptions {
    private List<String> groups;

    public IntegrationObjectOptions(List<String> groups) {
        this.groups = groups;
    }

    public List<String> getGroups() {
        return groups;
    }
}