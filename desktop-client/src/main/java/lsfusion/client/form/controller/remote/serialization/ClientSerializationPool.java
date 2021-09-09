package lsfusion.client.form.controller.remote.serialization;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.classes.ClientClassChooser;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.filter.ClientRegularFilter;
import lsfusion.client.form.filter.ClientRegularFilterGroup;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.client.form.object.table.grid.ClientGrid;
import lsfusion.client.form.object.table.grid.user.toolbar.ClientCalculations;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientAsyncAddRemove;
import lsfusion.client.form.property.async.ClientAsyncChange;
import lsfusion.client.form.property.async.ClientAsyncOpenForm;
import lsfusion.interop.form.remote.serialization.SerializationPool;


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
        addMapping(ClientGrid.class);
        addMapping(ClientToolbar.class);
        addMapping(ClientFilter.class);
        addMapping(ClientCalculations.class);
        addMapping(ClientClassChooser.class);
        addMapping(ClientObject.class);
        addMapping(ClientPropertyDraw.class);
        addMapping(ClientRegularFilter.class);
        addMapping(ClientRegularFilterGroup.class);
    }
}
