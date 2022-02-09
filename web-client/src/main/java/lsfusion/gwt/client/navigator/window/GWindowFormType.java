package lsfusion.gwt.client.navigator.window;

public enum GWindowFormType {
    FLOAT, DOCKED, EMBEDDED;

    public boolean isEmbedded() {
        return this == EMBEDDED;
    }
}
