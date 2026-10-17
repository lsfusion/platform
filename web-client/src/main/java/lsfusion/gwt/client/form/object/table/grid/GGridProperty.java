package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;

public abstract class GGridProperty extends GComponent {

    public int captionHeight;
    public int captionCharHeight;

    public int lineWidth;
    public int lineHeight;

    public GSize getCaptionHeight(boolean needNotNull) {
        return GPropertyDraw.getHeight(captionHeight, captionCharHeight, font, needNotNull);
    }

    protected abstract GGroupObject getLastGroup();
    protected abstract GSize getExtraWidth();

    @Override
    protected GSize getDefaultWidth() {
        return getDefaultSize().first;
    }

    @Override
    protected GSize getDefaultHeight() {
        return getDefaultSize().second;
    }

    protected Pair<GSize, GSize> getDefaultSize() {
        return getLastGroup().getSize(lineHeight, lineWidth, getExtraWidth(), getCaptionHeight(true));
    }

    public String valueClass;

    private class GValueElementClassReader extends GComponentReader {
        public GValueElementClassReader() {
            super(GGridProperty.this, "_GRID_VALUEELEMENTCLASSREADER");
        }

        @Override
        public void updatePlatform(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
            controller.getFormLayout().setValueClass(GGridProperty.this, PValue.getClassStringValue(values.get(GGroupObjectValue.EMPTY)));
        }
    }
    public final GPropertyReader valueElementClassReader = new GValueElementClassReader();
}
