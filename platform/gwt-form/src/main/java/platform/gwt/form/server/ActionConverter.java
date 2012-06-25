package platform.gwt.form.server;

import com.google.common.base.Throwables;
import platform.base.ReflectionUtils;
import platform.client.logics.ClientFormChanges;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.view.actions.*;
import platform.gwt.view.changes.dto.ObjectDTO;
import platform.interop.action.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

@SuppressWarnings("UnusedDeclaration")
public class ActionConverter {

    public GAction convert(FormSessionObject form, ClientAction action) throws IOException {
        if (action == null) {
            return null;
        }

        try {
            Method convertMethod = ReflectionUtils.getDeclaredMethodOrNull(ActionConverter.class, "convertAction", action.getClass());
            if (convertMethod != null) {
                return (GAction) convertMethod.invoke(this, action);
            }

            convertMethod = ReflectionUtils.getDeclaredMethodOrNull(ActionConverter.class, "convertAction", FormSessionObject.class, action.getClass());
            if (convertMethod != null) {
                return (GAction) convertMethod.invoke(this, form, action);
            }
        } catch (Exception e) {
            Throwables.propagateIfPossible(e, IOException.class);
            Throwables.propagate(e);
        }
        return null;
    }

    public GRequestUserInputAction convertAction(RequestUserInputClientAction action) throws IOException {
        return new GRequestUserInputAction(ClientTypeSerializer.deserialize(action.readType).getGwtType(), new ObjectDTO(action.oldValue));
    }

    public GMessageAction convertAction(MessageClientAction action) {
        return new GMessageAction(action.message, action.caption);
    }

    public GConfirmAction convertAction(ConfirmClientAction action) {
        return new GConfirmAction(action.message, action.caption);
    }

    public GProcessFormChangesAction convertAction(FormSessionObject form, ProcessFormChangesClientAction action) throws IOException {
        ClientFormChanges changes = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(action.formChanges)), form.clientForm);
        return new GProcessFormChangesAction(changes.getGwtFormChangesDTO());
    }
}
