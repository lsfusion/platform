package lsfusion.gwt.form.client.window;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GProgressBar implements IsSerializable {
    public String message;
    public int progress;
    public int total;
    public String params;

    public GProgressBar() {
    }

    public GProgressBar(String message, int progress, int total) {
        this(message, progress, total, null);
    }

    public GProgressBar(String message, int progress, int total, String params) {
        this.message = message;
        this.progress = progress;
        this.total = total;
        this.params = params;
    }

    @Override
    public String toString() {
        return message + ": " + progress + " of " + total;
    }
}