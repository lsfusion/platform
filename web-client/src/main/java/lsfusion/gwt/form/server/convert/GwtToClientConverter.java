package lsfusion.gwt.form.server.convert;

import com.google.common.base.Throwables;
import lsfusion.base.DateConverter;
import lsfusion.gwt.base.server.spring.BusinessLogicsProvider;
import lsfusion.gwt.form.server.FileUtils;
import lsfusion.gwt.form.shared.view.GClassViewType;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GUserInputResult;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.changes.dto.ColorDTO;
import lsfusion.gwt.form.shared.view.changes.dto.GDateDTO;
import lsfusion.gwt.form.shared.view.changes.dto.GFilesDTO;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FontInfo;
import lsfusion.interop.form.UserInputResult;
import sun.util.calendar.BaseCalendar;
import sun.util.calendar.CalendarSystem;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

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

    @Converter(from = Date.class)
    public java.sql.Date convertDate(Date date) {
        return DateConverter.safeDateToSql(date);
    }

    @Converter(from = GDateDTO.class)
    public java.sql.Date convertDate(GDateDTO gDate, BusinessLogicsProvider blProvider) {
        BaseCalendar calendar = CalendarSystem.getGregorianCalendar();
        BaseCalendar.Date date = (BaseCalendar.Date) calendar.newCalendarDate(blProvider.getTimeZone());
        date = date.setNormalizedDate(gDate.year + 1900, gDate.month + 1, gDate.day);
        return new java.sql.Date(calendar.getTime(date));
    }

    @Converter(from = GFont.class)
    public FontInfo convertFont(GFont fontInfo) {
        if (fontInfo == null) {
            return null;
        }
        return new FontInfo(fontInfo.family, fontInfo.size != null ? fontInfo.size : -1, fontInfo.bold, fontInfo.italic);
    }

    @Converter(from = GFilesDTO.class)
    public byte[] convertFiles(GFilesDTO filesObject) {
        return FileUtils.readFilesAndDelete(filesObject);
    }

    @Converter(from = GUserInputResult.class)
    public UserInputResult convertInputResult(GUserInputResult gInputResult, BusinessLogicsProvider blProvider) {
        return new UserInputResult(gInputResult.isCanceled(), convertOrCast(gInputResult.getValue(), blProvider));
    }

    @Converter(from = GClassViewType.class)
    public ClassViewType convertViewType(GClassViewType gViewType) {
        return ClassViewType.valueOf(gViewType.name());
    }

    @Converter(from = GGroupObjectValue.class)
    public byte[] convertGroupObjectValue(GGroupObjectValue groupObjectValue) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            int size = groupObjectValue.size();
            dataStream.writeInt(size);
            for (int i = 0; i < size; ++i) {
                dataStream.writeInt(groupObjectValue.getKey(i));
                serializeObject(dataStream, groupObjectValue.getValue(i));
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }
}
