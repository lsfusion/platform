package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;

import java.io.DataOutputStream;
import java.io.IOException;

public class ToolbarView extends BaseComponentView {
    public boolean visible = true;

    public boolean showViews = true;
    public boolean showFilters = true;
    public boolean showSettings = true;
    public boolean showCountQuantity = true;
    public boolean showCalculateSum = true;
    public boolean showPrintGroupXls = true;
    public boolean showManualUpdate = true;

    public ToolbarView(int ID) {
        super(ID);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(visible);

        outStream.writeBoolean(showViews);
        outStream.writeBoolean(showFilters);
        outStream.writeBoolean(showSettings);
        outStream.writeBoolean(showCountQuantity);
        outStream.writeBoolean(showCalculateSum);
        outStream.writeBoolean(showPrintGroupXls);
        outStream.writeBoolean(showManualUpdate);
    }

    @Override
    protected FlexAlignment getDefaultAlignment(FormInstanceContext context) {
        // we want the toolbar to always be on the same distance from the grid
        return FlexAlignment.START;
    }

    // copy-constructor
    public ToolbarView(ToolbarView src) {
        super(src);
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.visible = src.visible;
        this.showViews = src.showViews;
        this.showFilters = src.showFilters;
        this.showSettings = src.showSettings;
        this.showCountQuantity = src.showCountQuantity;
        this.showCalculateSum = src.showCalculateSum;
        this.showPrintGroupXls = src.showPrintGroupXls;
        this.showManualUpdate = src.showManualUpdate;
    }

    public void copy(ToolbarView src, ObjectMapping mapping) {
        super.copy(src, mapping);
    }
}
