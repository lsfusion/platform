package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.FormEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView {

    public boolean tabVertical = false;
    private boolean quickSearch = false;
    public int headerHeight = -1;

    public Integer lineWidth;
    public Integer lineHeight;

    public GroupObjectView groupObject;

    protected ContainerView record; // lazy creation, since its usage is pretty rear
    public ContainerView getRecord() { // assert that grid view is "finalized"
        return record;
    }
    @NFLazy
    public ContainerView getNFRecord(FormView formView) {
        if(record == null) {
            record = formView.createContainer();
            record.recordContainer = this;
        }
        return record;
    }

    public GridView() {
        
    }

    public GridView(int ID, GroupObjectView groupObject) {
        super(ID);
        this.groupObject = groupObject;
    }

    @Override
    public double getDefaultFlex(FormEntity formEntity) {
        return 1;
    }

    @Override
    public FlexAlignment getDefaultAlignment(FormEntity formEntity) {
        return FlexAlignment.STRETCH;
    }

    //todo: формально временное решение:
    //todo: метод дизайна, который изменяет энтити => должно быть перенсено на уровень энтити
    public void setQuickSearch(boolean quickSearch) {
        this.quickSearch = quickSearch;
        groupObject.entity.pageSize = 0;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(isAutoSize(pool.context.entity));

        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(quickSearch);
        outStream.writeInt(headerHeight);

        outStream.writeInt(getLineWidth());
        outStream.writeInt(getLineHeight());

        pool.serializeObject(outStream, getRecord());

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        autoSize = inStream.readBoolean();

        tabVertical = inStream.readBoolean();
        quickSearch = inStream.readBoolean();
        headerHeight = inStream.readInt();

        lineWidth = inStream.readInt();
        lineHeight = inStream.readInt();

        record = pool.deserializeObject(inStream);

        groupObject = pool.deserializeObject(inStream);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        if(record != null)
            record.finalizeAroundInit();
    }

    public int getLineWidth() {
        if(lineWidth != null)
            return lineWidth;

        return -1;
    }

    public void setLineWidth(Integer lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getLineHeight() {
        if(lineHeight != null)
            return lineHeight;

        return -1;
    }

    public void setLineHeight(Integer lineHeight) {
        this.lineHeight = lineHeight;
    }

    public Boolean autoSize;

    public boolean isAutoSize(FormEntity entity) {
        if(autoSize != null)
            return autoSize;

        return isCustom();
    }

    protected boolean isCustom() {
        return groupObject.entity.isCustom();
    }

    @Override
    protected int getDefaultWidth(FormEntity entity) {
        if(lineWidth == null && isCustom() && isAutoSize(entity))
            return 0;

        return super.getDefaultWidth(entity);
    }

    @Override
    protected int getDefaultHeight(FormEntity entity) {
        if(lineHeight == null && isCustom() && isAutoSize(entity))
            return 0;

        return super.getDefaultHeight(entity);
    }
}
