package lsfusion.http.provider.form;

import lsfusion.base.Pair;
import lsfusion.base.file.RawFileData;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.interop.form.FormClientData;
import lsfusion.interop.form.object.table.grid.user.design.ColumnUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.remote.RemoteFormInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static lsfusion.gwt.server.convert.StaticConverters.convertFont;

// session scoped - one for one browser (! not tab)
public class FormProviderImpl implements FormProvider, InitializingBean, DisposableBean {

    private final NavigatorProvider navigatorProvider;

    public FormProviderImpl(NavigatorProvider navigatorProvider) {
        this.navigatorProvider = navigatorProvider;
    }

    public GForm createForm(MainDispatchServlet servlet, RemoteFormInterface remoteForm, FormClientData clientData, String sessionID) throws IOException {
        // 0, 1, 3 are indices from FormClientAction.methodNames array
        ClientForm clientForm = ClientFormController.deserializeClientForm(remoteForm, clientData);
        Set<ClientGroupObject> inputGroupObjects = new HashSet<>();
        for(Integer inputObject : clientData.inputGroupObjects)
            inputGroupObjects.add(clientForm.getGroupObject(inputObject));
        clientForm.inputGroupObjects = inputGroupObjects;

        FormSessionObject formSessionObject = new FormSessionObject(clientForm, remoteForm, sessionID);

        GForm gForm = new ClientComponentToGwtConverter(servlet, formSessionObject).convertOrCast(clientForm);

        gForm.sID = clientData.formSID;
        gForm.canonicalName = clientData.formSID;

        byte[] firstChanges = clientData.firstChanges;
        if (firstChanges != null)
            gForm.initialFormChanges = ClientFormChangesToGwtConverter.getInstance().convertOrCast(
                    new ClientFormChanges(firstChanges, clientForm),
                    -1,
                    formSessionObject, servlet
            );

        FormUserPreferences formUP = clientData.userPreferences;
        if (formUP != null)
            gForm.userPreferences = new GFormUserPreferences(convertUserPreferences(gForm, formUP.getGroupObjectGeneralPreferencesList()),
                                                            convertUserPreferences(gForm, formUP.getGroupObjectUserPreferencesList()));

        String formID = nextFormSessionID();
        addFormSessionObject(formID, formSessionObject);
        gForm.sessionID = formID;
        return gForm;
    }

    public void createFormExternal(String formID, RemoteFormInterface remoteForm, String navigatorID) {
        addFormSessionObject(formID, new FormSessionObject(null, remoteForm, navigatorID));
    }

    private static List<GGroupObjectUserPreferences> convertUserPreferences(GForm gForm, List<GroupObjectUserPreferences> groupObjectUserPreferences) {
        ArrayList<GGroupObjectUserPreferences> gGroupObjectUPList = new ArrayList<>();
        for (GroupObjectUserPreferences groupObjectUP : groupObjectUserPreferences)
            gGroupObjectUPList.add(convertGroupUserPreferences(groupObjectUP, gForm));
        return gGroupObjectUPList;
    }

    private static GGroupObjectUserPreferences convertGroupUserPreferences(GroupObjectUserPreferences groupObjectUP, GForm gForm) {
        if(groupObjectUP == null)
            return null;

        HashMap<String, GColumnUserPreferences> gColumnUPMap = new HashMap<>();
        for (Map.Entry<String, ColumnUserPreferences> entry : groupObjectUP.getColumnUserPreferences().entrySet()) {
            ColumnUserPreferences columnUP = entry.getValue();
            gColumnUPMap.put(entry.getKey(), new GColumnUserPreferences(columnUP.userHide, columnUP.userCaption, columnUP.userPattern, columnUP.userWidth, columnUP.userFlex, columnUP.userOrder, columnUP.userSort, columnUP.userAscendingSort));
        }
        GFont userFont = convertFont(groupObjectUP.fontInfo);
        GGroupObject groupObj = gForm.getGroupObject(groupObjectUP.groupObjectSID);
        if (groupObj != null && groupObj.grid.font != null && groupObj.grid.font.size > 0) {
            if (userFont.size == 0) {
                userFont.size = groupObj.grid.font.size;
            }
            userFont.family = groupObj.grid.font.family;
        } else {
            userFont.family = "";
        }
        return new GGroupObjectUserPreferences(gColumnUPMap, groupObjectUP.groupObjectSID, userFont, groupObjectUP.pageSize, groupObjectUP.headerHeight, groupObjectUP.hasUserPreferences);
    }

    public FormSessionObject getFormSessionObject(String formSessionID) throws SessionInvalidatedException {
        FormSessionObject formSessionObject = currentForms.get(formSessionID);
        if(formSessionObject == null)
            throw new SessionInvalidatedException("Form " + formSessionID);
        return formSessionObject;
    }

    private final Map<String, FormSessionObject> currentForms = new ConcurrentHashMap<>();

    private AtomicInteger nextFormId = new AtomicInteger(0);
    private String nextFormSessionID() {
        return "form" + nextFormId.getAndIncrement();
    }
    private void addFormSessionObject(String formSessionID, FormSessionObject formSessionObject) {
        currentForms.put(formSessionID, formSessionObject);
    }

    private void removeFormSessionObject(String formSessionID) throws SessionInvalidatedException {
        FormSessionObject<?> sessionObject = getFormSessionObject(formSessionID);
        MainDispatchServlet.logger.error("Removing form " + formSessionID + "...");
        currentForms.remove(formSessionID);
        closeTempFiles(sessionObject);
    }

    private void closeTempFiles(FormSessionObject<?> sessionObject) {
        if (sessionObject.savedTempFiles != null) {
            for (Pair<String, Runnable> closer : sessionObject.savedTempFiles.values())
                closer.second.run();
        }
    }

    @Override
    public void removeFormSessionObjects(String sessionID) throws SessionInvalidatedException {
        Collection<String> formSessionIDs = new HashSet<>(currentForms.keySet());
        for (String formSessionID : formSessionIDs) {
            if (currentForms.get(formSessionID).navigatorID.equals(sessionID)) {
                try {
                    removeFormSessionObject(formSessionID); // maybe it's better to call remoteForm.close (just like navigators are closed in LogicsAndNavigatorProviderImpl), if there are opeened tabs (because if there are not such tabs, RemoteNavigator.close will do all the work) - but it is a rare case, so will do it later
                } catch (SessionInvalidatedException e) {
                }
            }
        }
    }

    private static final ScheduledExecutorService closeExecutor = Executors.newScheduledThreadPool(5);
    public void scheduleRemoveFormSessionObject(final String formSessionID, long delay) {
        closeExecutor.schedule(() -> {
            try {
                removeFormSessionObject(formSessionID);
            } catch (Throwable e) { // we need to suppress to not stop scheduler
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void destroy() throws Exception {
        for(FormSessionObject<?> sessionObject : currentForms.values()) {
            closeTempFiles(sessionObject);
        }
    }
}
