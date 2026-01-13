package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;

import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.nvl;

public class ToolbarView<AddGridParent extends GridPropertyView<AddGridParent, ?>> extends BaseGridComponentView<ToolbarView<AddGridParent>, AddGridParent> {
    private NFProperty<Boolean> visible = NFFact.property();

    private NFProperty<Boolean> showViews = NFFact.property();
    private NFProperty<Boolean> showFilters = NFFact.property();
    private NFProperty<Boolean> showSettings = NFFact.property();
    private NFProperty<Boolean> showCountQuantity = NFFact.property();
    private NFProperty<Boolean> showCalculateSum = NFFact.property();
    private NFProperty<Boolean> showPrintGroupXls = NFFact.property();
    private NFProperty<Boolean> showManualUpdate = NFFact.property();

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(isVisible());

        outStream.writeBoolean(isShowViews());
        outStream.writeBoolean(isShowFilters());
        outStream.writeBoolean(isShowSettings());
        outStream.writeBoolean(isShowCountQuantity());
        outStream.writeBoolean(isShowCalculateSum());
        outStream.writeBoolean(isShowPrintGroupXls());
        outStream.writeBoolean(isShowManualUpdate());
    }

    @Override
    protected FlexAlignment getDefaultAlignment(FormInstanceContext context) {
        // we want the toolbar to always be on the same distance from the grid
        return FlexAlignment.START;
    }

    public boolean isVisible() {
        return nvl(visible.get(), true);
    }
    public void setVisible(Boolean value, Version version) {
        visible.set(value, version);
    }

    public boolean isShowViews() {
        return nvl(showViews.get(), true);
    }
    public void setShowViews(Boolean value, Version version) {
        showViews.set(value, version);
    }

    public boolean isShowFilters() {
        return nvl(showFilters.get(), true);
    }
    public void setShowFilters(Boolean value, Version version) {
        showFilters.set(value, version);
    }

    public boolean isShowSettings() {
        return nvl(showSettings.get(), true);
    }
    public void setShowSettings(Boolean value, Version version) {
        showSettings.set(value, version);
    }

    public boolean isShowCountQuantity() {
        return nvl(showCountQuantity.get(), true);
    }
    public void setShowCountQuantity(Boolean value, Version version) {
        showCountQuantity.set(value, version);
    }

    public boolean isShowCalculateSum() {
        return nvl(showCalculateSum.get(), true);
    }
    public void setShowCalculateSum(Boolean value, Version version) {
        showCalculateSum.set(value, version);
    }

    public boolean isShowPrintGroupXls() {
        return nvl(showPrintGroupXls.get(), true);
    }
    public void setShowPrintGroupXls(Boolean value, Version version) {
        showPrintGroupXls.set(value, version);
    }

    public boolean isShowManualUpdate() {
        return nvl(showManualUpdate.get(), true);
    }
    public void setShowManualUpdate(Boolean value, Version version) {
        showManualUpdate.set(value, version);
    }

    public ToolbarView(IDGenerator idGen, AddGridParent groupView) {
        super(idGen, groupView);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();
        visible.finalizeChanges();
        showViews.finalizeChanges();
        showFilters.finalizeChanges();
        showSettings.finalizeChanges();
        showCountQuantity.finalizeChanges();
        showCalculateSum.finalizeChanges();
        showPrintGroupXls.finalizeChanges();
        showManualUpdate.finalizeChanges();
    }

    // copy-constructor
    protected ToolbarView(ToolbarView<AddGridParent> src, ObjectMapping mapping) {
        super(src, mapping);
    }
    @Override
    public void extend(ToolbarView<AddGridParent> src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(visible, src.visible);
        mapping.sets(showViews, src.showViews);
        mapping.sets(showFilters, src.showFilters);
        mapping.sets(showSettings, src.showSettings);
        mapping.sets(showCountQuantity, src.showCountQuantity);
        mapping.sets(showCalculateSum, src.showCalculateSum);
        mapping.sets(showPrintGroupXls, src.showPrintGroupXls);
        mapping.sets(showManualUpdate, src.showManualUpdate);
    }

    @Override
    public ToolbarView<AddGridParent> getAddChild(AddGridParent addGridParent, ObjectMapping mapping) {
        return addGridParent.toolbarSystem;
    }

    @Override
    public ToolbarView<AddGridParent> copy(ObjectMapping mapping) {
        return new ToolbarView<>(this, mapping);
    }
}
