package lsfusion.server.logics.form.interactive.design.property;

import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;

public interface PropertyGroupContainerView extends ServerIdentitySerializable {

    String getPropertyGroupContainerSID();

    String getPropertyGroupContainerName();

    DefaultFormView.ContainerSet getContainers();
}
