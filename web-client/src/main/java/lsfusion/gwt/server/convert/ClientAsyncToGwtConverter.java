package lsfusion.gwt.server.convert;

import lsfusion.client.form.property.async.ClientAsyncAddRemove;
import lsfusion.client.form.property.async.ClientAsyncChange;
import lsfusion.client.form.property.async.ClientAsyncOpenForm;
import lsfusion.client.form.property.async.ClientInputList;
import lsfusion.gwt.client.form.property.async.*;

public class ClientAsyncToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientAsyncToGwtConverter instance = new ClientAsyncToGwtConverter();
    }

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();

    public static ClientAsyncToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private ClientAsyncToGwtConverter() {
    }

    @Cached
    @Converter(from = ClientInputList.class)
    public GInputList convertInputList(ClientInputList inputList) {
        GAsyncExec[] actionAsyncs = new GAsyncExec[inputList.actionEvents.length];
        for(int i=0;i<inputList.actionEvents.length;i++)
            actionAsyncs[i] = convertOrCast(inputList.actionEvents[i]);
        return new GInputList(inputList.actions, actionAsyncs, inputList.strict);
    }

    @Cached
    @Converter(from = ClientAsyncAddRemove.class)
    public GAsyncAddRemove convertAsyncAddRemove(ClientAsyncAddRemove clientAddRemove) {
        return new GAsyncAddRemove(clientAddRemove.object, clientAddRemove.add);
    }

    @Cached
    @Converter(from = ClientAsyncChange.class)
    public GAsyncChange convertAsyncChange(ClientAsyncChange clientAsyncChange) {
        return new GAsyncChange(typeConverter.convertOrCast(clientAsyncChange.changeType), convertOrCast(clientAsyncChange.inputList));
    }

    @Cached
    @Converter(from = ClientAsyncOpenForm.class)
    public GAsyncOpenForm convertOpenForm(ClientAsyncOpenForm asyncOpenForm) {
        return new GAsyncOpenForm(asyncOpenForm.canonicalName, asyncOpenForm.caption, asyncOpenForm.forbidDuplicate, asyncOpenForm.modal, asyncOpenForm.window);
    }
}
