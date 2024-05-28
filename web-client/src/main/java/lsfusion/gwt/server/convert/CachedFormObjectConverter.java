package lsfusion.gwt.server.convert;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;

public class CachedFormObjectConverter extends CachedObjectConverter {

    protected final FormSessionObject formSessionObject;

    public CachedFormObjectConverter(MainDispatchServlet servlet, FormSessionObject formSessionObject) {
        super(servlet, formSessionObject.navigatorID);

        this.formSessionObject = formSessionObject;
    }
}
