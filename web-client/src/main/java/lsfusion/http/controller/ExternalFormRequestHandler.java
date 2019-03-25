package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.file.IOUtils;
import lsfusion.client.classes.ClientActionClass;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.gwt.client.GForm;
import lsfusion.http.provider.form.FormProvider;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.http.provider.navigator.NavigatorSessionObject;
import lsfusion.http.provider.session.SessionProvider;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.ProcessFormChangesClientAction;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteInternalException;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExternalUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.*;

import static lsfusion.gwt.server.form.FormActionHandler.defaultLastReceivedRequestIndex;

public class ExternalFormRequestHandler extends ExternalRequestHandler {

    @Autowired
    NavigatorProvider logicsAndNavigatorProvider;

    @Autowired
    SessionProvider sessionProvider;

    @Autowired
    FormProvider formProvider;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws RemoteException {
        String sessionID = null;
        boolean closeSession = false;
        try {
            String contentTypeString = request.getContentType();
            ContentType contentType = contentTypeString != null ? ContentType.parse(contentTypeString) : null;

            Charset charset = ExternalUtils.getCharsetFromContentType(contentType);
            JSONObject jsonObject = new JSONObject(new String(IOUtils.readBytesFromStream(request.getInputStream()), charset));

            sessionID = getSessionID(jsonObject);
            String action = jsonObject.optString("action");

            switch (action) {
                case "create": {
                    String name = jsonObject.optString("name");
                    String script = "run() { SHOW " + name + "; }";

                    NavigatorSessionObject navigatorSessionObject = logicsAndNavigatorProvider.getNavigatorSessionObject(sessionID);
                    if (navigatorSessionObject == null) {
                        sessionID = logicsAndNavigatorProvider.createNavigator(sessionObject, request);
                        navigatorSessionObject = logicsAndNavigatorProvider.getNavigatorSessionObject(sessionID);
                    }

                    ServerResponse serverResponse = navigatorSessionObject.remoteNavigator.executeNavigatorAction(script);
                    FormClientAction clientAction = getFormClientAction(serverResponse);
                    if (clientAction != null) {
                        GForm form = formProvider.createForm(clientAction.canonicalName, clientAction.formSID, clientAction.remoteForm, clientAction.immutableMethods, clientAction.firstChanges, sessionID);
                        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(clientAction.remoteForm.getRichDesignByteArray())));
                        ClientFormChanges formChanges = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(clientAction.firstChanges)), clientForm);

                        FormSessionObject formSessionObject = formProvider.getFormSessionObject(form.sessionID);
                        formSessionObject.currentGridObjects = formChanges.gridObjects;
                        sendResponse(response, new ExternalUtils.ExternalResponse(new StringEntity(createJSONResponse(formChanges.gridObjects, formChanges.properties, null, form.sessionID), charset), null, null, null, null, null));
                    } else {
                        sendErrorResponse(response, "Invalid response");
                    }
                    break;
                }
                case "change": {
                    JSONObject currentRows = jsonObject.optJSONObject("currentRows");
                    JSONObject data = jsonObject.optJSONObject("data");

                    FormSessionObject formSessionObject = formProvider.getFormSessionObject(sessionID);
                    if (currentRows != null && formSessionObject != null && formSessionObject.currentGridObjects != null) {

                        //changeGroupObject
                        Iterator<String> currentRowKeys = currentRows.keys();
                        while (currentRowKeys.hasNext()) {
                            String currentRowKey = currentRowKeys.next();
                            int currentRowValue = currentRows.getInt(currentRowKey);
                            changeGroupObject(formSessionObject, currentRowKey, currentRowValue);
                        }

                        Map<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> changeProperties = new HashMap<>();
                        Set<ClientPropertyDraw> dropProperties = new HashSet<>();

                        //changeProperty
                        if (data != null) {
                            Iterator<String> dataKeys = data.keys();
                            while (dataKeys.hasNext()) {
                                String dataKey = dataKeys.next();
                                Object dataValue = data.get(dataKey);

                                if (dataValue instanceof JSONObject) { //property with params
                                    Iterator<String> propertyKeys = ((JSONObject) dataValue).keys();
                                    while (propertyKeys.hasNext()) {
                                        String propertyName = propertyKeys.next();
                                        Object value = ((JSONObject) dataValue).get(propertyName);
                                        ClientFormChanges changes = changeProperty(formSessionObject, propertyName, value);
                                        if (changes != null) {
                                            changeProperties.putAll(changes.properties);
                                            dropProperties.addAll(changes.dropProperties);
                                        }
                                    }
                                } else { //property without params
                                    ClientFormChanges changes = changeProperty(formSessionObject, dataKey, dataValue);
                                    if (changes != null) {
                                        changeProperties.putAll(changes.properties);
                                        dropProperties.addAll(changes.dropProperties);
                                    }
                                }
                            }
                        }

                        String jsonResponse = createJSONResponse(formSessionObject.currentGridObjects, changeProperties, dropProperties, sessionID);
                        sendResponse(response, new ExternalUtils.ExternalResponse(new StringEntity(jsonResponse, charset), null, null, null, null, null));
                    } else {
                        sendErrorResponse(response, "Invalid change request");
                    }
                    break;
                }
                case "close": {
                    FormSessionObject formSessionObject = formProvider.getFormSessionObject(sessionID);
                    if (formSessionObject != null) {
                        formSessionObject.remoteForm.closeExternal();
                        formProvider.removeFormSessionObject(sessionID);
                        sendResponse(response, new ExternalUtils.ExternalResponse(new StringEntity("session closed", charset), null, null, null, null, null));
                    } else {
                        sendErrorResponse(response, "Invalid close request");
                    }
                    break;
                }
                default:
                    sendErrorResponse(response, "Unknown action: '" + action + "'");
            }


        } catch (Exception e) {
            if (e instanceof AuthenticationException) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/html; charset=utf-8");
                try { // in theory here can be changed exception (despite the fact that remote call is wrapped into RemoteExceptionAspect)
                    Pair<String, Pair<String, String>> actualStacks = RemoteInternalException.toString(e);
                    response.getWriter().print(actualStacks.first + '\n' + ExceptionUtils.getExStackTrace(actualStacks.second.first, actualStacks.second.second));
                } catch (IOException e1) {
                    throw Throwables.propagate(e1);
                }

                if (e instanceof RemoteException) { // rethrow RemoteException to invalidate LogicsSessionObject in LogicsProvider
                    closeSession = true; // closing session if there is a RemoteException
                    throw (RemoteException) e;
                }
            }
        } finally {
            if (sessionID != null && closeSession) {
                sessionProvider.removeSessionSessionObject(sessionID);
            }
        }
    }

    private String getSessionID(JSONObject jsonObject) {
        String sessionID = jsonObject.optString("sessionID");
        return sessionID.isEmpty() ? "external" : sessionID;
    }

    private void changeGroupObject(FormSessionObject formSessionObject, String currentRowKey, int currentRowValue) throws RemoteException {
        ClientGroupObject group = null;
        for (ClientGroupObject groupObject : formSessionObject.clientForm.groupObjects) {
            if (currentRowKey.equals(groupObject.getSID())) {
                group = groupObject;
            }
        }

        if (group != null) {
            List<ClientGroupObjectValue> gridObjects = null;
            for (Object entry : formSessionObject.currentGridObjects.entrySet()) {
                Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> currentGridObjectEntry = (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>>) entry;
                if (group.getSID().equals(currentGridObjectEntry.getKey().getSID())) {
                    gridObjects = currentGridObjectEntry.getValue();
                }
            }

            ClientGroupObjectValue groupObjectValue = gridObjects != null && gridObjects.size() > currentRowValue ? gridObjects.get(currentRowValue) : null;
            if (groupObjectValue != null) {
                formSessionObject.remoteForm.changeGroupObject(formSessionObject.requestIndex++, defaultLastReceivedRequestIndex, group.ID, groupObjectValue.serialize());
            }
        }
    }

    private ClientFormChanges changeProperty(FormSessionObject formSessionObject, String propertyFormName, Object value) throws IOException {
        ClientPropertyDraw property = null;
        for (ClientPropertyDraw propertyDraw : formSessionObject.clientForm.propertyDraws) {
            if (propertyFormName.equals(propertyDraw.getPropertyFormName())) {
                property = propertyDraw;
            }
        }

        if (property != null) {
            ServerResponse response = property.baseType instanceof ClientActionClass ?
                    formSessionObject.remoteForm.executeEditAction(formSessionObject.requestIndex++, defaultLastReceivedRequestIndex, property.ID, null, "change") :
                    formSessionObject.remoteForm.changePropertyExternal(formSessionObject.requestIndex++, defaultLastReceivedRequestIndex, property.ID, value instanceof String ? (String) value : String.valueOf(value));

            ProcessFormChangesClientAction formChangesAction = getProcessFormChangesClientAction(response);
            if(formChangesAction != null) {
                return new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(formChangesAction.formChanges)), formSessionObject.clientForm);
            }
        }
        return null;
    }

    private String createJSONResponse(Map<ClientGroupObject, List<ClientGroupObjectValue>> gridObjects, Map<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> properties, Set<ClientPropertyDraw> dropProperties, String formSessionID) {

        JSONObject dataJSON = new JSONObject();

        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> gridObject : gridObjects.entrySet()) {
            JSONArray gridObjectValues = new JSONArray();
            for (ClientGroupObjectValue clientGroupObjectValue : gridObject.getValue()) {

                JSONObject propValues = new JSONObject();
                for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> property : properties.entrySet()) {
                    String propertySID = getPropertyFormName(property.getKey());
                    if (propertySID != null) {
                        for (ClientGroupObjectValue propertyGroupObjectValue : property.getValue().keySet()) {
                            if (equalClientGroupObjectValues(propertyGroupObjectValue, clientGroupObjectValue)) {
                                Object propertyValue = property.getValue().get(propertyGroupObjectValue);
                                propValues.put(propertySID, propertyValue != null ? propertyValue : "null");
                            }
                        }
                    }
                }
                if(!propValues.isEmpty()) {
                    gridObjectValues.put(propValues);
                }

            }
            if(!gridObjectValues.isEmpty()) {
                dataJSON.put(gridObject.getKey().getSID(), gridObjectValues);
            }

            //properties without params
            for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> property : properties.entrySet()) {
                if (property.getKey().getGroupObject() == null) {
                    String propertySID = getPropertyFormName(property.getKey());
                    if(propertySID != null) {
                        Object propertyValue = property.getValue().get(ClientGroupObjectValue.EMPTY);
                        dataJSON.put(propertySID, propertyValue != null ? propertyValue : "null");
                    }
                }
            }
        }

        //drop properties
        if(dropProperties != null) {
            JSONArray dropPropertiesArray = new JSONArray();
            for (ClientPropertyDraw dropProperty : dropProperties) {
                dropPropertiesArray.put(dropProperty.getPropertyFormName());
            }
            if(!dropPropertiesArray.isEmpty()) {
                dataJSON.put("drop", dropPropertiesArray);
            }
        }

        JSONObject response = new JSONObject();
        response.put("sessionID", formSessionID);
        response.put("data", dataJSON);

        return response.toString();
    }

    private boolean equalClientGroupObjectValues(ClientGroupObjectValue map1, ClientGroupObjectValue map2) {
        if(map1.size() != map2.size())
            return false;
        List<ClientObject> keys1 = new ArrayList<>(map1.keySet());
        List<Object> values1 = new ArrayList<>(map1.values());
        List<ClientObject> keys2 = new ArrayList<>(map2.keySet());
        List<Object> values2 = new ArrayList<>(map2.values());
        for(int i = 0; i < keys1.size(); i++) {
            if(keys1.get(i).getID() != keys2.get(i).getID())
                return false;
        }
        for(int i = 0; i < values1.size(); i++) {
            if(!values1.equals(values2))
                return false;
        }
        return true;
    }

    private FormClientAction getFormClientAction(ServerResponse serverResponse) {
        FormClientAction formClientAction = null;
        if (serverResponse.actions != null) {
            for (ClientAction action : serverResponse.actions) {
                if (action instanceof FormClientAction) {
                    formClientAction = (FormClientAction) action;
                }
            }
        }
        return formClientAction;
    }

    private ProcessFormChangesClientAction getProcessFormChangesClientAction(ServerResponse serverResponse) {
        ProcessFormChangesClientAction formClientAction = null;
        if (serverResponse.actions != null) {
            for (ClientAction action : serverResponse.actions) {
                if (action instanceof ProcessFormChangesClientAction) {
                    formClientAction = (ProcessFormChangesClientAction) action;
                }
            }
        }
        return formClientAction;
    }

    private String getPropertyFormName(ClientPropertyReader property) {
        return property instanceof ClientPropertyDraw && !(((ClientPropertyDraw) property).baseType instanceof ClientActionClass) ? ((ClientPropertyDraw) property).getPropertyFormName() : null;
    }
}
