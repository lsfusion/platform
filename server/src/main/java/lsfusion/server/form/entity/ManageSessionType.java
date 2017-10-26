package lsfusion.server.form.entity;

public enum  ManageSessionType {
    AUTO, MANAGESESSION, NOMANAGESESSION;
    
    public boolean isManageSession() {
        assert this != AUTO;
        return this == MANAGESESSION;
    }
}
