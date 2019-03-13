package lsfusion.server.logics.form.interactive.action.edit;

public enum FormSessionScope {
    OLDSESSION, NEWSESSION, NESTEDSESSION;

    public boolean isNewSession() {
        return this == NEWSESSION || this == NESTEDSESSION;
    }
    public boolean isNestedSession() {
        return this == NESTEDSESSION;
    }
}
