package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.GetLocaleAction;
import lsfusion.interop.LocalePreferences;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class GetLocaleHandler extends NavigatorActionHandler<GetLocaleAction, StringResult> {
    public GetLocaleHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GetLocaleAction action, ExecutionContext context) throws DispatchException, IOException {
        LocalePreferences preferences = servlet.getNavigator().getLocalePreferences();
        return new StringResult(preferences.language == null ? "" : preferences.language);
    }
}