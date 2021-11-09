package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.language.ScriptParsingException;
import lsfusion.server.language.proxy.ViewProxyUtil;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.object.GridView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

import static lsfusion.interop.form.design.ContainerType.*;

public class ContainerView extends ComponentView {

    public NFOrderSet<ComponentView> children = NFFact.orderSet();

    public LocalizedString caption;

    private ContainerType type = ContainerType.CONTAINERV;
    private boolean horizontal;
    private boolean tabbed;

    public FlexAlignment childrenAlignment = FlexAlignment.START;

    private Boolean grid;
    private Boolean wrap;
    private Boolean alignCaptions;

    public int lines = 1;
    public Integer lineSize = null;

    public PropertyObjectEntity<?> showIf;

    // temp hack ???
    public GridView recordContainer;
    @Override
    public ComponentView getHiddenContainer() {
        ComponentView container = super.getHiddenContainer();
        if(recordContainer != null) {
            assert container == null;
            return recordContainer;
        }
        return container;
    }

    // extras
    public PropertyObjectEntity<?> propertyCaption;
    public PropertyObjectEntity<?> getExtra(ContainerViewExtraType type) {
        switch (type) {
            case CAPTION:
                return propertyCaption;
        }
        throw new UnsupportedOperationException();
    }

    public ContainerView() {
    }

    public ContainerView(int ID) {
        this(ID, false);
    }

    public boolean main;
    public ContainerView(int ID, boolean main) {
        super(ID);
        this.main = main;
    }

    public void setCaption(LocalizedString caption) {
        this.caption = caption;
    }

    public boolean isTabbed() {
        return type == ContainerType.TABBED_PANE || tabbed;
    }
    
    public boolean isScroll() {
        return type == ContainerType.SCROLL;
    }

    public boolean isSplitVertical() {
        return type == VERTICAL_SPLIT_PANE;
    }

    public boolean isSplitHorizontal() {
        return type == HORIZONTAL_SPLIT_PANE;
    }

    public boolean isSplit() {
        return isSplitHorizontal() || isSplitVertical();
    }
    
    public boolean isHorizontal() {
        return type == CONTAINERH || type == HORIZONTAL_SPLIT_PANE || horizontal;
    }

    public boolean isGrid() {
        if(grid != null)
            return grid;

        return false;
    }

    public Boolean isWrap() {
        if(wrap != null)
            return wrap;

        return lines > 1 || isHorizontal();
    }

    // we use Boolean since in desktop and in web there is a different default behaviour
    public Boolean isAlignCaptions() {
        return alignCaptions;
    }

    public void setType(ContainerType type) {
        if(type != COLUMNS && this.type == COLUMNS && lines > 1) { // temp check
            Supplier<DebugInfo.DebugPoint> debugPoint = ViewProxyUtil.setDebugPoint.get();
            ServerLoggers.startLogger.info("WARNING! Now container " + this + "  will have " + lines + " lines. Debug point : " + (debugPoint != null ? debugPoint.get() : "unknown"));
        }
        this.type = type;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }

    public void setTabbed(boolean tabbed) {
        this.tabbed = tabbed;
    }

    public void setChildrenAlignment(FlexAlignment childrenAlignment) {
        this.childrenAlignment = childrenAlignment;
    }

    public void setGrid(Boolean grid) {
        this.grid = grid;
    }

    public void setWrap(Boolean wrap) {
        this.wrap = wrap;
    }

    public void setAlignCaptions(boolean alignCaptions) {
        this.alignCaptions = alignCaptions;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public void setLineSize(Integer lineSize) {
        this.lineSize = lineSize;
    }

    public PropertyObjectEntity<?> getShowIf() {
        return showIf;
    }

    public void setShowIf(PropertyObjectEntity<?> showIf) {
        this.showIf = showIf;
    }

    @Override
    public ComponentView findById(int id) {
        ComponentView result = super.findById(id);
        if(result!=null) return result;
        
        for(ComponentView child : getChildrenIt()) {
            result = child.findById(id);
            if(result!=null) return result;
        }

        return null;
    }

    private void changeContainer(ComponentView comp, Version version) {
        comp.removeFromParent(version);
        comp.setContainer(this, version);
    }

    public void add(ComponentView comp) {
        add(comp, Version.descriptor());
    }
    
    public void add(ComponentView comp, Version version) {
        changeContainer(comp, version);
        children.add(comp, version);
    }

    public void addFirst(ComponentView comp, Version version) {
        changeContainer(comp, version);
        children.addFirst(comp, version);
    }

    public void addBefore(ComponentView comp, ComponentView compBefore, Version version) {
        changeContainer(comp, version);
        children.addIfNotExistsToThenLast(comp, compBefore, false, version);
    }

    public void addAfter(ComponentView comp, ComponentView compAfter, Version version) {
        changeContainer(comp, version);
        children.addIfNotExistsToThenLast(comp, compAfter, true, version);
    }

    public void fillPropertyContainers(MExclSet<ContainerView> mContainers) {
        if(showIf != null || propertyCaption != null)
            mContainers.exclAdd(this);

        for(ComponentView child : getChildrenIt())
            if(child instanceof ContainerView)
                ((ContainerView)child).fillPropertyContainers(mContainers);
    }

    public boolean isAncestorOf(ComponentView container) {
        return container != null && (super.isAncestorOf(container) || isAncestorOf(container.getHiddenContainer()));
    }

    public boolean isNFAncestorOf(ComponentView container, Version version) {
        return container != null && (super.isNFAncestorOf(container, version) || isNFAncestorOf(container.getNFContainer(version), version));
    }

    public Iterable<ComponentView> getChildrenIt() {
        return children.getIt();
    }
    public ImList<ComponentView> getChildrenList() {
        return children.getList();
    }
    public Iterable<ComponentView> getNFChildrenIt(Version version) {
        return children.getNFIt(version);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, getChildrenList());

        pool.writeString(outStream, ThreadLocalContext.localize(caption));

//        pool.writeObject(outStream, main);

        pool.writeBoolean(outStream, isHorizontal());
        pool.writeBoolean(outStream, isTabbed());

        pool.writeObject(outStream, childrenAlignment);
        
        outStream.writeBoolean(isGrid());
        outStream.writeBoolean(isWrap());
        pool.writeObject(outStream, alignCaptions);

        outStream.writeInt(lines);
        pool.writeInt(outStream, lineSize);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = NFFact.finalOrderSet(pool.deserializeList(inStream));

        caption = LocalizedString.create(pool.readString(inStream));

//        main = pool.readBoolean(inStream); // пока не будем делать, так как надо клиента обновлять

        horizontal = pool.readBoolean(inStream);
        tabbed = pool.readBoolean(inStream);

        childrenAlignment = pool.readObject(inStream);
        
        grid = inStream.readBoolean();
        wrap = inStream.readBoolean();
        alignCaptions = inStream.readBoolean();

        lines = inStream.readInt();
        lineSize = pool.readInt(inStream);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();
        
        for(ComponentView child : getChildrenIt())
            child.finalizeAroundInit();

        ImList<ComponentView> childrenList = getChildrenList();
        if (isSplit() && childrenList.size() != 2) {
            StringBuilder childrenString = new StringBuilder("");
            for (int i = 0; i < childrenList.size(); i++) {
                childrenString.append(childrenList.get(i).getSID());
                if (i != childrenList.size() - 1) {
                    childrenString.append(", ");
                }
            }
            throw new ScriptParsingException("Split container is allowed to have exactly two children:\n" +
                    "\tcontainer: " + getSID() + "\n" +
                    "\tchildren: " + childrenString.toString());
        }
    }

    @Override
    public String toString() {
        return (caption != null ? ThreadLocalContext.localize(caption) + " " : "") + super.toString();
    }
}
