package lsfusion.server.logics.form.interactive.design.property;

import lsfusion.server.logics.form.interactive.MappingInterface;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;

// assert that extends IdentityView<This>
public interface PropertyContainersView<This extends PropertyContainersView<This>> extends MappingInterface<This> {

    DefaultFormView.ContainerSet getContainers();
}
