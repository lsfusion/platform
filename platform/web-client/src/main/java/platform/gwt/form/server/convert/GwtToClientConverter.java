package platform.gwt.form.server.convert;

import com.google.common.base.Throwables;
import platform.base.DateConverter;
import platform.gwt.form.shared.view.GClassViewType;
import platform.gwt.form.shared.view.GUserInputResult;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.changes.dto.ColorDTO;
import platform.gwt.form.shared.view.changes.dto.GFilesDTO;
import platform.interop.ClassViewType;
import platform.interop.form.UserInputResult;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import static platform.base.BaseUtils.serializeObject;

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

    @Converter(from = GFilesDTO.class)
    public byte[] convertFiles(GFilesDTO filesObject) {
        return FileManager.readFilesAndDelete(filesObject);
    }

    @Converter(from = GUserInputResult.class)
    public UserInputResult convertInputResult(GUserInputResult gInputResult) {
        return new UserInputResult(gInputResult.isCanceled(), convertOrCast(gInputResult.getValue()));
    }

    @Converter(from = GClassViewType.class)
    public ClassViewType conertViewType(GClassViewType gViewType) {
        return ClassViewType.valueOf(gViewType.name());
    }

    @Converter(from = GGroupObjectValue.class)
    public byte[] conertGroupObjectValue(GGroupObjectValue groupObjectValue) {
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
