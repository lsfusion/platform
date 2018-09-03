package lsfusion.server.form.entity;

import lsfusion.server.logics.property.group.AbstractGroup;

import java.util.List;

public class IntegrationPropertyOptions {
    private AbstractGroup group;
    private List<String> groups;
    private Boolean attr;

    public IntegrationPropertyOptions(AbstractGroup group, List<String> groups, Boolean attr) {
        this.group = group;
        this.groups = groups;
        this.attr = attr;
    }

    public AbstractGroup getGroup() {
        return group;
    }

    public List<String> getGroups() {
        return groups;
    }

    public Boolean getAttr() {
        return attr;
    }
}