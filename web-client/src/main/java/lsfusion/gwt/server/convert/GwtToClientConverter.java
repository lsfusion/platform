package lsfusion.gwt.server.convert;

import com.google.common.base.Throwables;
import lsfusion.client.form.object.ClientCustomObjectValue;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.async.ClientPushAsyncAdd;
import lsfusion.client.form.property.async.ClientPushAsyncClose;
import lsfusion.client.form.property.async.ClientPushAsyncInput;
import lsfusion.gwt.client.GFormScheduler;
import lsfusion.gwt.client.action.GExternalHttpResponse;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GCustomObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.client.form.property.GPropertyGroupType;
import lsfusion.gwt.client.form.property.async.GPushAsyncAdd;
import lsfusion.gwt.client.form.property.async.GPushAsyncClose;
import lsfusion.gwt.client.form.property.async.GPushAsyncInput;
import lsfusion.gwt.client.form.property.cell.classes.*;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.server.FileUtils;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.event.FormScheduler;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.object.table.grid.user.design.ColumnUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.interop.form.property.cell.UserInputResult;
import lsfusion.interop.session.ExternalHttpResponse;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.BaseUtils.serializeObject;

@SuppressWarnings("UnusedDeclaration")
public class GwtToClientConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final GwtToClientConverter instance = new GwtToClientConverter();
    }

    public static GwtToClientConverter getInstance() {
        return InstanceHolder.instance;
    }

    private GwtToClientConverter() {
    }

    @Converter(from = ColorDTO.class)
    public Color convertColorDTO(ColorDTO dto) {
        int c = Integer.parseInt(dto.value, 16);
        return new Color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
    }

    @Converter(from = GDateDTO.class)
    public LocalDate convertDate(GDateDTO dto) {
        return LocalDate.of(dto.year, dto.month, dto.day);
    }

    @Converter(from = GTimeDTO.class)
    public LocalTime convertTime(GTimeDTO dto) {
        return LocalTime.of(dto.hour, dto.minute, dto.second);
    }

    @Converter(from = GDateTimeDTO.class)
    public LocalDateTime convertDateTime(GDateTimeDTO dto) {
        return LocalDateTime.of(dto.year, dto.month, dto.day, dto.hour, dto.minute, dto.second);
    }

    @Converter(from = GZDateTimeDTO.class)
    public Instant convertDateTime(GZDateTimeDTO dto) {
        return Instant.ofEpochMilli(dto.instant);
    }

    @Converter(from = GFont.class)
    public FontInfo convertFont(GFont fontInfo) {
        if (fontInfo == null) {
            return null;
        }
        return new FontInfo(fontInfo.family, fontInfo.size, fontInfo.bold, fontInfo.italic);
    }

    @Converter(from = GFilesDTO.class)
    public Object convertFiles(GFilesDTO filesObject) {
        return FileUtils.readUploadFileAndDelete(filesObject);
    }

    @Converter(from = GUserInputResult.class)
    public UserInputResult convertInputResult(GUserInputResult gInputResult) {
        return new UserInputResult(gInputResult.isCanceled(), convertOrCast(gInputResult.getValue()), gInputResult.getContextAction());
    }

    @Converter(from = GClassViewType.class)
    public ClassViewType convertViewType(GClassViewType gViewType) {
        return ClassViewType.valueOf(gViewType.name());
    }

    @Converter(from = GListViewType.class)
    public ListViewType convertViewType(GListViewType gViewType) {
        return ListViewType.valueOf(gViewType.name());
    }

    @Converter(from = GPropertyGroupType.class)
    public PropertyGroupType convertGroupType(GPropertyGroupType gViewType) {
        return PropertyGroupType.valueOf(gViewType.name());
    }

    @Converter(from = GUpdateMode.class)
    public UpdateMode convertGroupType(GUpdateMode gViewType) {
        return UpdateMode.valueOf(gViewType.name());
    }

    @Converter(from = GGroupObjectValue.class)
    public byte[] convertGroupObjectValue(GGroupObjectValue groupObjectValue) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        serializeGroupObjectValue(dataStream, groupObjectValue);

        return outStream.toByteArray();
    }

    // should correspond AsyncChange.deserializePush(byte[])
    @Converter(from = GPushAsyncAdd.class)
    public byte[] convertPushASyncAdd(GPushAsyncAdd pushAsyncChange) {
        return new ClientPushAsyncAdd(pushAsyncChange.ID).serialize();
    }
    @Converter(from = GPushAsyncInput.class)
    public byte[] convertPushAsyncChange(GPushAsyncInput pushAsync) {
        return new ClientPushAsyncInput(convertOrCast(pushAsync.result)).serialize();
    }
    @Converter(from = GPushAsyncClose.class)
    public byte[] convertPushASyncClose(GPushAsyncClose pushAsyncChange) {
        return new ClientPushAsyncClose().serialize();
    }

    @Converter(from = GExternalHttpResponse.class)
    public ExternalHttpResponse convertCustomObjectValue(GExternalHttpResponse gResponse) {
        return new ExternalHttpResponse(gResponse.contentType, gResponse.responseBytes, gResponse.responseHeaders, gResponse.statusCode, gResponse.statusText);
    }

    @Converter(from = GCustomObjectValue.class)
    public ClientCustomObjectValue convertCustomObjectValue(GCustomObjectValue gValue) {
        return new ClientCustomObjectValue(gValue.id, gValue.getIdClass());
    }

    public void serializeGroupObjectValue(DataOutputStream dataStream, GGroupObjectValue groupObjectValue) {
        try {
            int size = groupObjectValue.size();
            dataStream.writeInt(size);
            for (int i = 0; i < size; ++i) {
                dataStream.writeInt(groupObjectValue.getKey(i));
                ClientGroupObjectValue.serializeObjectValue(dataStream, convertOrCast(groupObjectValue.getValue(i)));
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    @Converter(from = GFormUserPreferences.class)
    public FormUserPreferences convertFormUserPreferences(GFormUserPreferences gprefs) {
        java.util.List<GroupObjectUserPreferences> generalPrefs = new ArrayList<>();
        java.util.List<GroupObjectUserPreferences> userPrefs = new ArrayList<>();
        for (GGroupObjectUserPreferences prefs : gprefs.getGroupObjectGeneralPreferencesList()) {
            generalPrefs.add(convertGroupObjectPreferences(prefs));
        }
        for (GGroupObjectUserPreferences prefs : gprefs.getGroupObjectUserPreferencesList()) {
            userPrefs.add(convertGroupObjectPreferences(prefs));
        }
        return new FormUserPreferences(generalPrefs, userPrefs);    
    }
    
    @Converter(from = GGroupObjectUserPreferences.class)
    public GroupObjectUserPreferences convertGroupObjectPreferences(GGroupObjectUserPreferences gprefs) {
        if(gprefs == null)
            return null;

        Map<String, ColumnUserPreferences> columnUPs = new HashMap<>();
        for (Map.Entry<String, GColumnUserPreferences> entry : gprefs.getColumnUserPreferences().entrySet()) {
            columnUPs.put(entry.getKey(), convertColumnPreferences(entry.getValue()));
        }
        return new GroupObjectUserPreferences(columnUPs, gprefs.getGroupObjectSID(), convertFont(gprefs.getFont()), gprefs.getPageSize(), gprefs.getHeaderHeight(), gprefs.hasUserPreferences());
    }
    
    @Converter(from = GColumnUserPreferences.class)
    public ColumnUserPreferences convertColumnPreferences(GColumnUserPreferences gprefs) {
        return new ColumnUserPreferences(gprefs.userHide, gprefs.userCaption, gprefs.userPattern, gprefs.userWidth, gprefs.userFlex, gprefs.userOrder, gprefs.userSort, gprefs.userAscendingSort);
    }

    @Converter(from = GFormScheduler.class)
    public FormScheduler convertFormScheduler(GFormScheduler formScheduler) {
        return new FormScheduler(formScheduler.period, formScheduler.fixed);
    }
}
