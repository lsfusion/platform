package lsfusion.gwt.client.navigator.window;

public class GContainerWindowFormType implements GWindowFormType {

    Integer inContainerId;

    @SuppressWarnings("unused")
    public GContainerWindowFormType() {
    }

    public GContainerWindowFormType(Integer inContainerId) {
        this.inContainerId = inContainerId;
    }

    public Integer getInContainerId() {
        return inContainerId;
    }
}