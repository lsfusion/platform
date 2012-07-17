package platform.gwt.form2.server;

import platform.gwt.base.shared.GClassViewType;
import platform.gwt.view2.GUserInputResult;
import platform.gwt.view2.changes.dto.ColorDTO;
import platform.interop.ClassViewType;
import platform.interop.form.UserInputResult;

import java.awt.*;

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
        return Color.decode(dto.value);
    }

    @Converter(from = GUserInputResult.class)
    public UserInputResult convertInputResult(GUserInputResult gInputResult) {
        return new UserInputResult(gInputResult.isCanceled(), convertOrCast(gInputResult.getValue()));
    }

    @Converter(from = GClassViewType.class)
    public ClassViewType conertViewType(GClassViewType gViewType) {
        return ClassViewType.valueOf(gViewType.name());
    }
}
