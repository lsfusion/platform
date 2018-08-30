package lsfusion.server.form.entity;

import java.util.List;

public class IntegrationPropertyOptions {
    private List<String> groups;
    private Boolean attr;

    public IntegrationPropertyOptions(List<String> groups, Boolean attr) {
        this.groups = groups;
        this.attr = attr;
    }

    public List<String> getGroups() {
        return groups;
    }

    public Boolean getAttr() {
        return attr;
    }
}