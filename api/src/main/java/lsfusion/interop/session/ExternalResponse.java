package lsfusion.interop.session;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

public abstract class ExternalResponse implements Serializable {
    public int getStatusHttp() {
        return HttpServletResponse.SC_OK;
    }
}
