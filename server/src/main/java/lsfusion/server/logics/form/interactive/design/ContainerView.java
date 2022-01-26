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
import lsfusion.server.logics.form.struct.FormEntity;
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
    public Boolean collapsible;

    private ContainerType type = ContainerType.CONTAINERV;
    private boolean horizontal;
    private boolean tabbed;

    public FlexAlignment childrenAlignment = FlexAlignment.START;

    private Boolean grid;
    private Boolean wrap;
    private Boolean alignCaptions;

    public int lines = 1;
    public Integer lineSize = null;
    public Boolean lineShrink = null;

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
    
    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
    }
    
    public boolean isCollapsible() {
        if(collapsible != null)
            return collapsible;

        return isDefaultCollapsible();
    }

    protected boolean isDefaultCollapsible() {
        return !PropertyDrawView.hasNoCaption(caption, propertyCaption);
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

    @Override
    public boolean isDefaultShrink(FormEntity formEntity, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        boolean horizontal = container != null && container.isHorizontal();

        if(isShrinkedAutoSizedWrap(formEntity, horizontal))
            return true;

        if(!explicit && container != null && container.isWrap() && isShrinkDominant(formEntity, container, horizontal, false))
            return true;

        return super.isDefaultShrink(formEntity, explicit);
    }

    public boolean isDefaultAlignShrink(FormEntity formEntity, boolean explicit) {
        ContainerView container = getLayoutParamContainer();
        boolean horizontal = container != null && container.isHorizontal();
        if(isShrinkedAutoSizedWrap(formEntity, !horizontal))
            return true;

        if(!explicit && container != null && isShrinkDominant(formEntity, container, !horizontal, true))
            return true;

        return super.isDefaultAlignShrink(formEntity, explicit);
    }

    public boolean isLineShrink(FormEntity formEntity) {
        if(lineShrink != null)
            return lineShrink;

        // if we're shrinking this container, it makes sense to shrink lines too (because they are sort of virtual containers)
        ContainerView container = getLayoutParamContainer();
        boolean horizontal = container != null && container.isHorizontal();
        boolean linesHorizontal = !isHorizontal(); // lines direction
        boolean sameDirection = horizontal == linesHorizontal;
        return sameDirection ? isShrink(formEntity) : isAlignShrink(formEntity);
    }

    private boolean isShrinkDominant(FormEntity formEntity, ContainerView container, boolean horizontal, boolean align) {
        ContainerView upperContainer = container.getLayoutParamContainer();
        boolean upperHorizontal = upperContainer != null && upperContainer.isHorizontal();
        if((horizontal == upperHorizontal ? container.isShrink(formEntity) : container.isAlignShrink(formEntity))) {
            // checking siblings if there are more
            int shrinked = 0;
            int notShrinked = 0;
            for(ComponentView child : container.getChildrenIt())
                if(align ? child.isAlignShrink(formEntity, true) : child.isShrink(formEntity, true))
                    shrinked++;
                else
                    notShrinked++;
            if(shrinked > notShrinked)
                 return true;
        }
        return false;
    }

    // if we have cascade shrinking (with auto size) and some wrap at some point, consider that we want shrink
    // otherwise shrinking will lead to more scrolls in lower containers
    // however we can use simple shrink check
    protected boolean isShrinkedAutoSizedWrap(FormEntity formEntity, boolean horizontal) {
        if ((horizontal ? getWidth(formEntity) : getHeight(formEntity)) != -1) // if we have fixed size than there is no wrap problem
            return false;

        boolean thisHorizontal = isHorizontal();
        // now there are several heuristics at the web client changing the default behaviour, and disabling wrap
        // most of them are grid related, so we just disable shrink in grid for now
        if (isWrap() && !isGrid()) {
            boolean wrapHorizontal = (thisHorizontal == (lines == 1));
            return wrapHorizontal == horizontal; // if there is wrap and it's in required direction that's what we are looking for
            // important if it's wrong direction wrap, we should not use children since it will break this heuristics (it doesn't make sense when wrap "goes" to the upper containers)
        }

        boolean sameDirection = horizontal == thisHorizontal;
        for (ComponentView child : getChildrenList())
            if(child instanceof ContainerView) {
                ContainerView containerChild = (ContainerView) child;
                if ((sameDirection ? containerChild.isShrink(formEntity, true) : child.isAlignShrink(formEntity, true)) && containerChild.isShrinkedAutoSizedWrap(formEntity, horizontal))
                    return true;
            }

        return false;
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

    public void setLineShrink(boolean lineShrink) {
        this.lineShrink = lineShrink;
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

        outStream.writeBoolean(isCollapsible());
//        pool.writeObject(outStream, main);

        pool.writeBoolean(outStream, isHorizontal());
        pool.writeBoolean(outStream, isTabbed());

        pool.writeObject(outStream, childrenAlignment);
        
        outStream.writeBoolean(isGrid());
        outStream.writeBoolean(isWrap());
        pool.writeObject(outStream, alignCaptions);

        outStream.writeInt(lines);
        pool.writeInt(outStream, lineSize);
        outStream.writeBoolean(isLineShrink(pool.context.entity));
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = NFFact.finalOrderSet(pool.deserializeList(inStream));

        caption = LocalizedString.create(pool.readString(inStream));
        
        collapsible = inStream.readBoolean();

//        main = pool.readBoolean(inStream); // пока не будем делать, так как надо клиента обновлять

        horizontal = pool.readBoolean(inStream);
        tabbed = pool.readBoolean(inStream);

        childrenAlignment = pool.readObject(inStream);
        
        grid = inStream.readBoolean();
        wrap = inStream.readBoolean();
        alignCaptions = inStream.readBoolean();

        lines = inStream.readInt();
        lineSize = pool.readInt(inStream);
        lineShrink = inStream.readBoolean();
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
