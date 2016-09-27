package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.shared.actions.navigator.GetLocaleAction;
import lsfusion.interop.LocalePreferences;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class GetLocaleHandler extends SimpleActionHandlerEx<GetLocaleAction, StringResult, RemoteLogicsInterface> implements NavigatorActionHandler {
    public GetLocaleHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GetLocaleAction action, ExecutionContext context) throws DispatchException, IOException {
        LocalePreferences defaultPreferences = servlet.getLogics().getDefaultLocalePreferences();
        LocalePreferences userPreferences = servlet.getNavigator().getLocalePreferences();
        LocalePreferences preferences = LocalePreferences.overrideDefaultWithUser(defaultPreferences, userPreferences);
        return new StringResult(preferences.language == null ? "" : preferences.language);
    }
}