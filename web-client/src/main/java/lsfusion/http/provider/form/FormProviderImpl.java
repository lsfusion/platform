package lsfusion.http.provider.form;

import lsfusion.client.form.ClientForm;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
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
import java.util.concurrent.atomic.AtomicInteger;

import static lsfusion.gwt.server.convert.StaticConverters.convertFont;

// session scoped - one for one browser (! not tab)
public class FormProviderImpl implements FormProvider, InitializingBean, DisposableBean {

    @Autowired
    private NavigatorProvider navigatorProvider;
    
    public FormProviderImpl() {}

    public GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String sessionID) throws IOException {
        // 0 and 2 are indices from FormClientAction.methodNames array
        byte[] formDesign = immutableMethods != null ? (byte[]) immutableMethods[2] : remoteForm.getRichDesignByteArray();
        FormUserPreferences formUP = immutableMethods != null ? (FormUserPreferences)immutableMethods[0] : remoteForm.getUserPreferences();

        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(formDesign)));

        GForm gForm = new ClientComponentToGwtConverter(navigatorProvider.getLogicsName(sessionID)).convertOrCast(clientForm);

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

        gForm.sessionID = addFormSessionObject(formSessionObject);
        return gForm;
    }

    private static List<GGroupObjectUserPreferences> convertUserPreferences(GForm gForm, List<GroupObjectUserPreferences> groupObjectUserPreferences) {
        ArrayList<GGroupObjectUserPreferences> gGroupObjectUPList = new ArrayList<>();
        for (GroupObjectUserPreferences groupObjectUP : groupObjectUserPreferences) {
            HashMap<String, GColumnUserPreferences> gColumnUPMap = new HashMap<>();
            for (Map.Entry<String, ColumnUserPreferences> entry : groupObjectUP.getColumnUserPreferences().entrySet()) {
                ColumnUserPreferences columnUP = entry.getValue();
                gColumnUPMap.put(entry.getKey(), new GColumnUserPreferences(columnUP.userHide, columnUP.userCaption, columnUP.userPattern, columnUP.userWidth, columnUP.userOrder, columnUP.userSort, columnUP.userAscendingSort));
            }
            GFont userFont = convertFont(groupObjectUP.fontInfo);
            GGroupObject groupObj = gForm.getGroupObject(groupObjectUP.groupObjectSID);
            if (groupObj != null && groupObj.grid.font != null && groupObj.grid.font.size != 0) {
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
            gGroupObjectUPList.add(new GGroupObjectUserPreferences(gColumnUPMap, groupObjectUP.groupObjectSID, userFont, groupObjectUP.pageSize, groupObjectUP.headerHeight, groupObjectUP.hasUserPreferences));
            gForm.addFont(userFont); // добавляем к используемым шрифтам с целью подготовить FontMetrics
        }
        return gGroupObjectUPList;
    }

    public FormSessionObject getFormSessionObject(String formSessionID) {
        return currentForms.get(formSessionID);
    }

    private final Map<String, FormSessionObject> currentForms = new ConcurrentHashMap<>();

    private AtomicInteger nextFormId = new AtomicInteger(0);
    private String nextFormSessionID() {
        return "form" + nextFormId.getAndIncrement();
    }
    private String addFormSessionObject(FormSessionObject formSessionObject) {
        String formSessionID = nextFormSessionID();
        currentForms.put(formSessionID, formSessionObject);
        return formSessionID;
    }

    public void removeFormSessionObject(String formSessionID) {
        FormSessionObject<?> sessionObject = currentForms.remove(formSessionID);
        for(File file : sessionObject.savedTempFiles)
            FileUtils.deleteFile(file);        
    }

    @Override
    public void removeFormSessionObjects(String sessionID) {
        Collection<String> formSessionIDs = new HashSet<>(currentForms.keySet());
        for (String formSessionID : formSessionIDs) {
            if (currentForms.get(formSessionID).sessionID.equals(sessionID)) {
                removeFormSessionObject(formSessionID); // maybe it's better to call remoteForm.close (just like navigators are closed in LogicsAndNavigatorProviderImpl), if there are opeened tabs (because if there are not such tabs, RemoteNavigator.close will do all the work) - but it is a rare case, so will do it later 
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void destroy() throws Exception {
    }
}
