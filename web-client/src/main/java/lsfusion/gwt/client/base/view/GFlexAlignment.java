package lsfusion.gwt.client.base.view;

public enum GFlexAlignment {
    START, CENTER, END, STRETCH;

    public boolean isShrink() {
        return this == STRETCH;
    }
}
