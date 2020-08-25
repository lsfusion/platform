package lsfusion.http.provider.form;

import lsfusion.client.form.ClientForm;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.interop.form.object.table.grid.user.design.ColumnUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.remote.RemoteFormInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
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

    @Autowired
    private NavigatorProvider navigatorProvider;
    
    public FormProviderImpl() {}

    public GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String sessionID) throws IOException {
        // 0, 1, 3 are indices from FormClientAction.methodNames array
        byte[] formDesign = immutableMethods != null ? (byte[]) immutableMethods[1] : remoteForm.getRichDesignByteArray();
        FormUserPreferences formUP = immutableMethods != null ? (FormUserPreferences)immutableMethods[0] : remoteForm.getUserPreferences();

        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(formDesign)));

        GForm gForm = new ClientComponentToGwtConverter(navigatorProvider.getLogicsName(sessionID)).convertOrCast(clientForm);

        Set<Integer> inputObjects = remoteForm.getInputGroupObjects();
        gForm.inputGroupObjects = new HashSet<>();
        for(Integer inputObject : inputObjects) {
            gForm.inputGroupObjects.add(gForm.getGroupObject(inputObject));
        }

        gForm.sID = formSID;
        gForm.canonicalName = canonicalName;

        FormSessionObject formSessionObject = new FormSessionObject(clientForm, remoteForm, sessionID);

        if (firstChanges != null)
            gForm.initialFormChanges = ClientFormChangesToGwtConverter.getInstance().convertOrCast(
                    new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(firstChanges)), clientForm),
                    -1,
                    formSessionObject                    
            );

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
            gColumnUPMap.put(entry.getKey(), new GColumnUserPreferences(columnUP.userHide, columnUP.userCaption, columnUP.userPattern, columnUP.userWidth, columnUP.userOrder, columnUP.userSort, columnUP.userAscendingSort));
        }
        GFont userFont = convertFont(groupObjectUP.fontInfo);
        GGroupObject groupObj = gForm.getGroupObject(groupObjectUP.groupObjectSID);
        if (groupObj != null && groupObj.grid.font != null && groupObj.grid.font.size > 0) {
            if (userFont.size == 0) {
                userFont.size = groupObj.grid.font.size;
            }
            userFont.family = groupObj.grid.font.family;
        } else {
            if (userFont.size == 0) {
                userFont.size = GFont.DEFAULT_FONT_SIZE;
            }
            userFont.family = GFont.DEFAULT_FONT_FAMILY;
        }
        gForm.addFont(userFont); // добавляем к используемым шрифтам с целью подготовить FontMetrics
        return new GGroupObjectUserPreferences(gColumnUPMap, groupObjectUP.groupObjectSID, userFont, groupObjectUP.pageSize, groupObjectUP.headerHeight, groupObjectUP.hasUserPreferences);
    }

    public FormSessionObject getFormSessionObject(String formSessionID) throws SessionInvalidatedException {
        FormSessionObject formSessionObject = currentForms.get(formSessionID);
        if(formSessionObject == null)
            throw new SessionInvalidatedException();
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
        currentForms.remove(formSessionID);
        if(sessionObject.savedTempFiles != null) {
            for (File file : sessionObject.savedTempFiles)
                FileUtils.deleteFile(file);
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
    }
}
