package lsfusion.gwt.server.convert;

import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.client.form.property.async.*;
import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.async.*;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.navigator.window.GContainerWindowFormType;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.interop.form.ContainerWindowFormType;
import lsfusion.interop.form.ModalityWindowFormType;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.logics.ServerSettings;

import javax.servlet.ServletContext;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

public class ClientAsyncToGwtConverter extends CachedObjectConverter {

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();
    private final ClientFormChangesToGwtConverter valuesConverter = ClientFormChangesToGwtConverter.getInstance();

    public ClientAsyncToGwtConverter(MainDispatchServlet servlet, String sessionID) {
        super(servlet, sessionID);
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
                convertOrCast(clientInputListAction.keyStroke), convertOrCast(clientInputListAction.editingBindingMode),
                quickAccessList, clientInputListAction.index);
    }

    @Converter(from = KeyStroke.class)
    public GKeyStroke convertKeyStroke(KeyStroke keyStroke) {
        int modifiers = keyStroke.getModifiers();
        boolean isAltPressed = (modifiers & InputEvent.ALT_MASK) != 0;
        boolean isCtrlPressed = (modifiers & InputEvent.CTRL_MASK) != 0;
        boolean isShiftPressed = (modifiers & InputEvent.SHIFT_MASK) != 0;
        int keyCode = convertKeyCode(keyStroke.getKeyCode());

        return new GKeyStroke(keyCode, isAltPressed, isCtrlPressed, isShiftPressed);
    }

    @Converter(from = BindingMode.class)
    public GBindingMode convertBindingMode(BindingMode bindingMode) {
        return  GBindingMode.valueOf(bindingMode.name());
    }

    private int convertKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_DELETE:
                return KeyCodes.KEY_DELETE;
            case KeyEvent.VK_ESCAPE:
                return KeyCodes.KEY_ESCAPE;
            case KeyEvent.VK_ENTER:
                return KeyCodes.KEY_ENTER;
            case KeyEvent.VK_INSERT:
                return GKeyStroke.KEY_INSERT;
            default:
                return keyCode;
        }
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
    public GAsyncChange convertAsyncChange(ClientAsyncChange clientAsyncChange) {
        return new GAsyncChange(clientAsyncChange.propertyIDs, valuesConverter.convertOrCast(clientAsyncChange.value));
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
