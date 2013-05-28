package platform.server.session;

public enum ChangeType {
    FINAL, NOUPDATE, NOTFINAL;

    public boolean isFinal() {
        return this==FINAL || this==NOUPDATE;
    }
}
