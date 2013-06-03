package lsfusion.server.form.instance;

public enum FormSessionScope {
    OLDSESSION, NEWSESSION, MANAGESESSION;

    public boolean isNewSession() {
        return this == NEWSESSION;
    }

    public boolean isManageSession() {
        return this != OLDSESSION;
    }
}
