package lsfusion.server.form.entity;

public enum  ManageSessionType {
    AUTO, MANAGESESSION, NOMANAGESESSION, MANAGESESSIONX, NOMANAGESESSIONX;
    
    public boolean isManageSession() {
        assert this != AUTO;
        return this == MANAGESESSIONX || this == MANAGESESSION;
    }
    public boolean isX() {
        assert this != AUTO;
        return this == MANAGESESSIONX || this == NOMANAGESESSIONX;
    }
}
