package lsfusion.interop.form;

public class ContainerShowFormType implements ShowFormType {

    public Integer inContainerId;

    public ContainerShowFormType(Integer inContainerId) {
        this.inContainerId = inContainerId;
    }

    @Override
    public Integer getInContainerId() {
        return inContainerId;
    }

    @Override
    public WindowFormType getWindowType() {
        return WindowFormType.INNER;
    }

    @Override
    public String getName() {
        return "INNER";
    }
}