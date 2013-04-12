package platform.gwt.paas.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.PlaceManagerImpl;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;

public class PaasPlaceManager extends PlaceManagerImpl {
    public static final String TARGET_PARAM = "targetPlace";

    private final PlaceRequest defaultPlaceRequest = new PlaceRequest(NameTokens.defaultPage);

    private final PlaceRequest errorPlaceReqest = new PlaceRequest(NameTokens.errorPage);

    private final PlaceRequest projectListPage = new PlaceRequest(NameTokens.projectsListPage);
    private final TokenFormatter tokenFormatter;

    @Inject
    public PaasPlaceManager(EventBus eventBus, TokenFormatter tokenFormatter) {
        super(eventBus, tokenFormatter);
        this.tokenFormatter = tokenFormatter;
    }

    @Override
    public void revealDefaultPlace() {
        revealPlace(defaultPlaceRequest);
    }

    @Override
    public void revealErrorPlace(String invalidHistoryToken) {
        revealPlace(errorPlaceReqest);
    }

    public void revealProjectListPage() {
        revealPlace(projectListPage);
    }

    @Override
    public void revealUnauthorizedPlace(String unauthorizedHistoryToken) {
        Log.debug("Unauthorized access to: " + unauthorizedHistoryToken);
        revealPlace(new PlaceRequest(NameTokens.loginPage).with(TARGET_PARAM, unauthorizedHistoryToken));
    }

    public void revealPlaceFromString(String target) {
        revealPlace(tokenFormatter.toPlaceRequest(target));
    }

    public String getCurrentParameter(String parameterName, String defaultValue) {
        return getCurrentPlaceRequest().getParameter(parameterName, defaultValue);
    }

    public int getCurrentIntParameter(String parameterName, int defaultValue) {
        String value = getCurrentParameter(parameterName, null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }
}
