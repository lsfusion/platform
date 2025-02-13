package lsfusion.gwt.server.convert;

import lsfusion.client.form.property.async.*;
import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;
import lsfusion.gwt.client.form.property.async.*;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.navigator.window.GContainerWindowFormType;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.ContainerWindowFormType;
import lsfusion.interop.form.ModalityWindowFormType;

import java.io.IOException;

import java.util.ArrayList;

public class ClientAsyncToGwtConverter extends CachedFormObjectConverter {

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();
    private final ClientFormChangesToGwtConverter valuesConverter = ClientFormChangesToGwtConverter.getInstance();
    private final ClientBindingToGwtConverter bindingConverter = ClientBindingToGwtConverter.getInstance();

    public ClientAsyncToGwtConverter(MainDispatchServlet servlet, FormSessionObject formSessionObject) {
        super(servlet, formSessionObject);
    }

    @Cached
    @Converter(from = ClientInputList.class)
    public GInputList convertInputList(ClientInputList inputList) {
        return new GInputList(convertOrCast(inputList.completionType), null);
    }

    @Cached
    @Converter(from = ClientInputListAction[].class)
    public GInputListAction[] convertInputList(ClientInputListAction[] actions) {
        GInputListAction[] result = new GInputListAction[actions.length];
        for(int i = 0; i < actions.length;i++) {
            result[i] = convertOrCast(actions[i]);
        }
        return result;
    }

    @Cached
    @Converter(from = ClientInputListAction.class)
    public GInputListAction convertInputListAction(ClientInputListAction clientInputListAction) throws IOException {
        ArrayList<GQuickAccess> quickAccessList = new ArrayList<>();
        for(int i = 0; i < clientInputListAction.quickAccessList.size(); i++) {
            quickAccessList.add(convertOrCast(clientInputListAction.quickAccessList.get(i)));
        }
        return new GInputListAction(createImage(clientInputListAction.action, false), clientInputListAction.id, convertOrCast(clientInputListAction.asyncExec),
                clientInputListAction.keyStroke != null ? bindingConverter.convertKeyStroke(clientInputListAction.keyStroke) : null,
                clientInputListAction.editingBindingMode != null ? bindingConverter.convertBindingMode(clientInputListAction.editingBindingMode) : null,
                quickAccessList, clientInputListAction.index);
    }

    @Converter(from = ClientQuickAccess.class)
    public GQuickAccess convertQuickAccess(ClientQuickAccess quickAccess) {
        return new GQuickAccess(convertOrCast(quickAccess.mode), quickAccess.hover);
    }

    @Converter(from = ClientQuickAccessMode.class)
    public GQuickAccessMode convertQuickAccessMode(ClientQuickAccessMode quickAccess) {
        switch (quickAccess) {
            case ALL: return GQuickAccessMode.ALL;
            case SELECTED: return GQuickAccessMode.SELECTED;
            case FOCUSED: return GQuickAccessMode.FOCUSED;
        }
        return null;
    }

    @Cached
    @Converter(from = ClientAsyncAddRemove.class)
    public GAsyncAddRemove convertAsyncAddRemove(ClientAsyncAddRemove clientAddRemove) {
        return new GAsyncAddRemove(clientAddRemove.object, clientAddRemove.add);
    }

    @Cached
    @Converter(from = ClientAsyncInput.class)
    public GAsyncInput convertAsyncInput(ClientAsyncInput clientAsyncInput) {
        return new GAsyncInput(typeConverter.convertOrCast(clientAsyncInput.changeType), convertOrCast(clientAsyncInput.inputList), convertOrCast(clientAsyncInput.inputListActions), clientAsyncInput.customEditorFunction);
    }

    @Cached
    @Converter(from = ClientAsyncChange.class)
    public GAsyncChange convertAsyncChange(ClientAsyncChange clientAsyncChange) throws IOException {
        return new GAsyncChange(clientAsyncChange.propertyIDs, valuesConverter.convertFileValue(clientAsyncChange.value, formSessionObject, servlet));
    }

    @Converter(from = ModalityWindowFormType.class)
    public GModalityWindowFormType convertModalityWindowFormType(ModalityWindowFormType modalityWindowFormType) {
        switch (modalityWindowFormType) {
            case DOCKED: return GModalityWindowFormType.DOCKED;
            case FLOAT: return GModalityWindowFormType.FLOAT;
            case EMBEDDED: return GModalityWindowFormType.EMBEDDED;
            case POPUP: return GModalityWindowFormType.POPUP;
        }
        return null;
    }

    @Converter(from = ContainerWindowFormType.class)
    public GContainerWindowFormType convertContainerWindowFormType(ContainerWindowFormType containerWindowFormType) {
        return new GContainerWindowFormType(containerWindowFormType.inContainerId);
    }

    @Cached
    @Converter(from = ClientAsyncOpenForm.class)
    public GAsyncOpenForm convertOpenForm(ClientAsyncOpenForm asyncOpenForm) throws IOException {
        return new GAsyncOpenForm(asyncOpenForm.canonicalName, asyncOpenForm.caption, createImage(asyncOpenForm.appImage, false), asyncOpenForm.forbidDuplicate, asyncOpenForm.modal, convertOrCast(asyncOpenForm.type));
    }

    @Cached
    @Converter(from = ClientAsyncCloseForm.class)
    public GAsyncCloseForm convertCloseForm(ClientAsyncCloseForm asyncCloseForm) {
        return new GAsyncCloseForm();
    }

    @Cached
    @Converter(from = ClientAsyncNoWaitExec.class)
    public GAsyncNoWaitExec convertNoWaitExec(ClientAsyncNoWaitExec asyncNoWaitExec) {
        return new GAsyncNoWaitExec();
    }
    
    @Cached
    @Converter(from = CompletionType.class)
    public GCompletionType convertCompletionType(CompletionType completionType) {
        switch (completionType) {
            case STRICT:
                return GCompletionType.STRICT;
            case NON_STRICT:
                return GCompletionType.NON_STRICT;
            case SEMI_STRICT:
                return GCompletionType.SEMI_STRICT;
        }
        return null;
    } 
}
