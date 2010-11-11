package platform.client.descriptor.editor.base;

public enum Tristate {
    NOT_SELECTED, SELECTED, MIXED;

    public Tristate and(Tristate state) {
        if(equals(state))
            return this;
        else
            return MIXED;
    }
}
