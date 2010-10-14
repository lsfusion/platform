package platform.client.form;

import platform.client.logics.ClientObject;
import platform.client.logics.ClientPropertyDraw;

import java.util.List;

public interface LogicsSupplier {

    List<ClientObject> getObjects();
    List<ClientPropertyDraw> getPropertyDraws();
}
