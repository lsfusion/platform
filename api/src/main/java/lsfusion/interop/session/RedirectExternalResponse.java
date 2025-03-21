package lsfusion.interop.session;

public class RedirectExternalResponse extends ExternalResponse {

    public final String url;
    public final Integer notification;
    public final String[] usedParams;

    public RedirectExternalResponse(String url, Integer notification, String[] usedParams) {
        this.url = url;
        this.notification = notification;
        this.usedParams = usedParams;
    }
}
