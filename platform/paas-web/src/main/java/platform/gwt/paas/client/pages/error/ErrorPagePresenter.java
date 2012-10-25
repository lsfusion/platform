package platform.gwt.paas.client.pages.error;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.web.bindery.event.shared.EventBus;
import com.google.gwt.user.client.ui.Button;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.*;
import platform.gwt.paas.client.NameTokens;

public class ErrorPagePresenter extends Presenter<ErrorPagePresenter.MyView, ErrorPagePresenter.MyProxy> {

    private final PlaceManager placeManager;

    @ProxyCodeSplit
    @NameToken(NameTokens.errorPage)
    @NoGatekeeper
    public interface MyProxy extends Proxy<ErrorPagePresenter>, Place {
    }

    public interface MyView extends View {
        Button getOkButton();
    }

    @Inject
    public ErrorPagePresenter(EventBus eventBus, MyView view, MyProxy proxy,
                              PlaceManager placeManager) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(getView().getOkButton().addClickHandler(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        PlaceRequest myRequest = new PlaceRequest(NameTokens.defaultPage);
                        placeManager.revealPlace(myRequest);
                    }
                }));
    }

    @Override
    protected void revealInParent() {
        // RevealRootLayoutContentEvent.fire(this, this);
        RevealRootContentEvent.fire(this, this);
    }
}