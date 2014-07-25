package lsfusion.client.serialization;

import lsfusion.base.context.ApplicationContext;
import lsfusion.base.serialization.SerializationPool;
import lsfusion.client.logics.*;


public class ClientSerializationPool extends SerializationPool<ClientForm> {


    public ClientSerializationPool() {
        this(null, null);
    }

    public ClientSerializationPool(ApplicationContext appContext) {
        this(null, appContext);
    }

    public ClientSerializationPool(ClientForm context, ApplicationContext appContext) {
        super(context, appContext);
        //порядок добавления должен соответствовать порядку в ServerSerializationPool

        addMapping(ClientForm.class);
        addMapping(ClientComponent.class);
        addMapping(ClientContainer.class);
        addMapping(ClientGroupObject.class);
        addMapping(ClientTreeGroup.class);
        addMapping(ClientShowType.class);
        addMapping(ClientGrid.class);
        addMapping(ClientToolbar.class);
        addMapping(ClientFilter.class);
        addMapping(ClientClassChooser.class);
        addMapping(ClientObject.class);
        addMapping(ClientPropertyDraw.class);
        addMapping(ClientRegularFilter.class);
        addMapping(ClientRegularFilterGroup.class);
    }
}
