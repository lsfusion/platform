package lsfusion.interop.session;

public class RedirectExternalResponse extends ExternalResponse {

    public final String url;
    public final Integer notification;

    public RedirectExternalResponse(String url) {
        this(url, null);
    }
    public RedirectExternalResponse(String url, Integer notification) {
        this.url = url;
        this.notification = notification;
    }
}
