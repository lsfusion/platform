package lsfusion.gwt.server.form.form.provider;

import lsfusion.client.logics.ClientForm;
import lsfusion.client.logics.ClientFormChanges;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.gwt.server.form.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.server.form.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.shared.form.view.*;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import lsfusion.interop.form.RemoteFormInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static lsfusion.gwt.server.form.convert.StaticConverters.convertFont;

// session scoped - one for one browser (! not tab)
public class FormProviderImpl implements FormProvider, InitializingBean, DisposableBean {

    public FormProviderImpl() {}

    public GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String sessionID) throws IOException {
        // 0 and 2 are indices from FormClientAction.methodNames array
        byte[] formDesign = immutableMethods != null ? (byte[]) immutableMethods[2] : remoteForm.getRichDesignByteArray();
        FormUserPreferences formUP = immutableMethods != null ? (FormUserPreferences)immutableMethods[0] : remoteForm.getUserPreferences();

        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(formDesign)));

        GForm gForm = new ClientComponentToGwtConverter().convertOrCast(clientForm);

        gForm.sID = formSID;
        gForm.canonicalName = canonicalName;

        if (firstChanges != null)
            gForm.initialFormChanges = ClientFormChangesToGwtConverter.getInstance().convertOrCast(
                    new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(firstChanges)), clientForm),
                    -1
            );

        if (formUP != null)
            gForm.userPreferences = new GFormUserPreferences(convertUserPreferences(gForm, formUP.getGroupObjectGeneralPreferencesList()),
                                                            convertUserPreferences(gForm, formUP.getGroupObjectUserPreferencesList()));

        gForm.sessionID = addFormSessionObject(new FormSessionObject(clientForm, remoteForm, sessionID));
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
        currentForms.remove(formSessionID);
    }

    @Override
    public void removeFormSessionObjects(String sessionID) {
        Collection<String> formSessionIDs = new HashSet<>(currentForms.keySet());
        for (String formSessionID : formSessionIDs) {
            if (currentForms.get(formSessionID).sessionID.equals(sessionID)) {
                currentForms.remove(formSessionID); // по хорошему надо вызывать remoteForm.close (по аналогии с RemoteNavigator), если остались открытые вкладки (так как если их нет, всю работу выполнит RemoteNavigator.close) - но это редкий и нестандартный случай так что пока делать не будем
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
