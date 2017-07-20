package lsfusion.gwt.form.shared.view.grid;

import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.base.client.GwtClientUtils;

public class NativeEditEvent extends EditEvent {
    private final NativeEvent nativeEvent;

    public NativeEditEvent(NativeEvent nativeEvent) {
        this.nativeEvent = nativeEvent;
    }

    public NativeEvent getNativeEvent() {
        return nativeEvent;
    }

    @Override
    public void stopPropagation() {
        GwtClientUtils.stopPropagation(nativeEvent);
    }
}
