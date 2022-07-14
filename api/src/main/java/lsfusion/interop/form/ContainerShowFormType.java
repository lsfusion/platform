package lsfusion.interop.form;

public class ContainerShowFormType implements ShowFormType {

    public Integer inContainerId;

    public ContainerShowFormType(Integer inContainerId) {
        this.inContainerId = inContainerId;
    }

    @Override
    public WindowFormType getWindowType() {
        return new ContainerWindowFormType(inContainerId);
    }
}