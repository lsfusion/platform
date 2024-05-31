package lsfusion.server.logics.form.interactive.controller.remote.serialization;

public class ConnectionContext {

    // dynamic part
    public final boolean useBootstrap;
    public final boolean contentWordWrap;

    public ConnectionContext(boolean useBootstrap, boolean contentWordWrap) {
        this.useBootstrap = useBootstrap;
        this.contentWordWrap = contentWordWrap;
    }
}
