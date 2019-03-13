package lsfusion.server.logics.form.interactive;

public enum  ManageSessionType {
    AUTO, MANAGESESSION, NOMANAGESESSION;
    
    public boolean isManageSession() {
        assert this != AUTO;
        return this == MANAGESESSION;
    }
}
