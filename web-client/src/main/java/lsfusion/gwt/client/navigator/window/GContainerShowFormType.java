package lsfusion.gwt.client.navigator.window;

public class GContainerShowFormType implements GShowFormType {

    public Integer inContainerId;

    @SuppressWarnings("unused")
    public GContainerShowFormType() {
    }

    public GContainerShowFormType(Integer inContainerId) {
        this.inContainerId = inContainerId;
    }

    @Override
    public Integer getInContainerId() {
        return inContainerId;
    }

    @Override
    public boolean isWindow() {
        return false;
    }

    @Override
    public GWindowFormType getWindowType() {
        return GWindowFormType.INNER;
    }
}