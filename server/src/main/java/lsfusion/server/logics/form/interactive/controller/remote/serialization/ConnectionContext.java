package lsfusion.server.logics.form.interactive.controller.remote.serialization;

public class ConnectionContext {

    // dynamic part
    public final boolean useBootstrap;
    public final boolean contentWordWrap;
    public final boolean highlightDuplicateValue;
    public final boolean isNative;

    public ConnectionContext(boolean useBootstrap, boolean contentWordWrap, boolean highlightDuplicateValue, boolean isNative) {
        this.useBootstrap = useBootstrap;
        this.contentWordWrap = contentWordWrap;
        this.highlightDuplicateValue = highlightDuplicateValue;
        this.isNative = isNative;
    }
}
