package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.converters.KeyStrokeConverter;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrPropertyUtils;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.awt.*;

public class PropertyDrawViewProxy extends ComponentViewProxy<PropertyDrawView> {

    public PropertyDrawViewProxy(PropertyDrawView target) {
        super(target);
    }

    @SuppressWarnings("unused")
    public void setAutoSize(boolean autoSize) {
        Version version = getVersion();
        Integer valueWidth = target.getNFValueWidth(version);
        Integer valueHeight = target.getNFValueHeight(version);
        if(valueWidth == null || valueWidth < 0)
            target.setValueWidth(autoSize ? -1 : -2, version);
        if(valueHeight == null || valueHeight < 0)
            target.setValueHeight(autoSize ? -1 : -2, version);
    }

    @SuppressWarnings("unused")
    public void setChangeOnSingleClick(boolean changeOnSingleClick) {
        target.setChangeOnSingleClick(changeOnSingleClick, getVersion());
    }

    @SuppressWarnings("unused")
    public void setMaxValue(long maxValue) {
        target.setMaxValue(maxValue, getVersion());
    }

    @SuppressWarnings("unused")
    public void setEchoSymbols(boolean echoSymbols) {
        target.setEchoSymbols(echoSymbols, getVersion());
    }

    @SuppressWarnings("unused")
    public void setNoSort(boolean noSort) {
        target.setNoSort(noSort, getVersion());
    }

    @SuppressWarnings("unused")
    public void setDefaultCompare(String defaultCompare) {
        target.setDefaultCompare(ActionOrPropertyUtils.stringToCompare(defaultCompare), getVersion());
    }

    @SuppressWarnings("unused")
    public void setCharWidth(int charWidth) {
        target.setCharWidth(charWidth, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCharHeight(int charHeight) {
        target.setCharHeight(charHeight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueSize(Dimension size) {
        target.setValueWidth(size.width, getVersion());
        target.setValueHeight(size.height, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueWidth(int valueWidth) {
        target.setValueWidth(valueWidth, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueHeight(int valueHeight) {
        target.setValueHeight(valueHeight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionWidth(int captionWidth) {
        target.setCaptionWidth(captionWidth, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionHeight(int captionHeight) {
        target.setCaptionHeight(captionHeight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionCharHeight(int captionCharHeight) {
        target.setCaptionCharHeight(captionCharHeight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueFlex(boolean valueFlex) {
        target.setValueFlex(valueFlex, getVersion());
    }

    @SuppressWarnings("unused")
    public void setTag(LocalizedString tag) {
        target.setTag(tag.getSourceString(), getVersion());
    }

    @SuppressWarnings("unused")
    public void setInputType(LocalizedString inputType) {
        target.setInputType(inputType.getSourceString(), getVersion());
    }

    @SuppressWarnings("unused")
    public void setPanelCustom(Boolean panelCustom) {
        target.setPanelCustom(panelCustom, getVersion());
    }

    @SuppressWarnings("unused")
    public void setChangeKey(Object changeKey) {
        Version version = getVersion();
        if(changeKey instanceof LocalizedString) {
            target.setChangeKey(KeyStrokeConverter.parseInputBindingEvent(changeKey.toString(), false), version);
        } else {
            if (target.getNFChangeKey(version) == null) {
                //dumb value will be replaced with a dynamic one
                target.setChangeKey(InputBindingEvent.dumb, version);
            }
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) changeKey, PropertyDrawExtraType.CHANGEKEY, version);
        }
    }

    //deprecated
    @SuppressWarnings("unused")
    public void setChangeKeyPriority(int priority) {
        InputBindingEvent changeKey = target.getNFChangeKey(getVersion());
        if(changeKey != null)
            changeKey.priority = priority;
    }

    @SuppressWarnings("unused")
    public void setShowChangeKey(boolean showChangeKey) {
        target.setShowChangeKey(showChangeKey,  getVersion());
    }

    @SuppressWarnings("unused")
    public void setChangeMouse(Object changeMouse) {
        Version version = getVersion();
        if(changeMouse instanceof LocalizedString) {
            target.setChangeMouse(KeyStrokeConverter.parseInputBindingEvent(changeMouse.toString(), true), version);
        } else {
            if (target.getNFChangeMouse(version) == null) {
                //dumb value will be replaced with a dynamic one.
                target.setChangeMouse(InputBindingEvent.dumb,  version);
            }
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) changeMouse, PropertyDrawExtraType.CHANGEMOUSE, version);
        }
    }

    //deprecated
    @SuppressWarnings("unused")
    public void setChangeMousePriority(int priority) {
        InputBindingEvent changeMouse = target.getNFChangeMouse(getVersion());
        if (changeMouse != null)
            changeMouse.priority = priority;
    }

    @SuppressWarnings("unused")
    public void setShowChangeMouse(boolean showChangeMouse) {
        target.setShowChangeMouse(showChangeMouse, getVersion());
    }

    @SuppressWarnings("unused")
    public void setInline(Boolean inline) {
        target.setInline(inline, getVersion());
    }

    @SuppressWarnings("unused")
    public void setFocusable(Boolean focusable) {
        target.setFocusable(focusable, getVersion());;
    }

    @SuppressWarnings("unused")
    public void setPanelColumnVertical(boolean panelColumnVertical) {
        target.setPanelColumnVertical(panelColumnVertical, getVersion());
    }

    //deprecated
    @SuppressWarnings("unused")
    public void setValueAlignment(FlexAlignment valueAlignment) {
        setValueAlignmentHorz(valueAlignment);
    }

    @SuppressWarnings("unused")
    public void setValueAlignmentHorz(FlexAlignment valueAlignmentHorz) {
        target.setValueAlignmentHorz(valueAlignmentHorz, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueAlignmentVert(FlexAlignment valueAlignmentVert) {
        target.setValueAlignmentVert(valueAlignmentVert, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueOverflowHorz(String valueOverflowHorz) {
        target.setValueOverflowHorz(valueOverflowHorz, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueOverflowVert(String valueOverflowVert) {
        target.setValueOverflowVert(valueOverflowVert, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueShrinkHorz(Boolean valueShrinkHorz) {
        target.setValueShrinkHorz(valueShrinkHorz, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueShrinkVert(Boolean valueShrinkVert) {
        target.setValueShrinkVert(valueShrinkVert, getVersion());
    }

    @SuppressWarnings("unused")
    public void setComment(Object comment) {
        Version version = getVersion();
        if (comment instanceof LocalizedString)
            target.setComment((LocalizedString) comment, version);
        else {
            if (target.getNFComment(version) == null)
                target.setComment(LocalizedString.NONAME, version);
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) comment, PropertyDrawExtraType.COMMENT, version);
        }
    }

    @SuppressWarnings("unused")
    public void setCommentClass(Object valueClass) {
        Version version = getVersion();
        if(valueClass instanceof LocalizedString)
            target.setCommentElementClass(((LocalizedString) valueClass).getSourceString(), version);
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueClass, PropertyDrawExtraType.COMMENTELEMENTCLASS, version);
    }

    @SuppressWarnings("unused")
    public void setPanelCommentVertical(boolean panelCommentVertical) {
        target.setPanelCommentVertical(panelCommentVertical, getVersion());
    }

    @SuppressWarnings("unused")
    public void setPanelCommentFirst(boolean panelCommentFirst) {
        target.setPanelCommentFirst(panelCommentFirst, getVersion());
    }

    @SuppressWarnings("unused")
    public void setPanelCommentAlignment(FlexAlignment panelCommentAlignment) {
        target.setPanelCommentAlignment(panelCommentAlignment, getVersion());
    }

    @SuppressWarnings("unused")
    public void setHide(boolean hide) {
        target.entity.setHide(hide, getVersion());
    }

    @SuppressWarnings("unused")
    public void setClass(Object elementClass) {
        super.setClass(elementClass);
        if(elementClass instanceof PropertyObjectEntity)
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) elementClass, PropertyDrawExtraType.GRIDELEMENTCLASS, getVersion());
    }

    @SuppressWarnings("unused")
    public void setValueClass(Object valueClass) {
        if(valueClass instanceof LocalizedString)
            target.setValueElementClass(((LocalizedString) valueClass).getSourceString(), getVersion());
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueClass, PropertyDrawExtraType.VALUEELEMENTCLASS, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionClass(Object valueClass) {
        if(valueClass instanceof LocalizedString)
            target.setCaptionElementClass(((LocalizedString) valueClass).getSourceString(), getVersion());
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueClass, PropertyDrawExtraType.CAPTIONELEMENTCLASS, getVersion());
    }

    public void setFooterClass(Object valueClass) {
        if(valueClass instanceof LocalizedString)
            target.setFooterElementClass(((LocalizedString) valueClass).getSourceString(), getVersion());
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueClass, PropertyDrawExtraType.FOOTERELEMENTCLASS, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaption(Object caption) {
        if(caption instanceof LocalizedString)
            target.entity.setCaption((LocalizedString) caption, getVersion());
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) caption, PropertyDrawExtraType.CAPTION, getVersion());
    }

    @SuppressWarnings("unused")
    public void setImagePath(LocalizedString image) {
        setImage(image);
    }

    @SuppressWarnings("unused")
    public void setImage(Object image) {
        if(image instanceof LocalizedString)
            target.entity.setImage(((LocalizedString) image).getSourceString(), getVersion());
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) image, PropertyDrawExtraType.IMAGE, getVersion());
    }

    @SuppressWarnings("unused")
    public void setPlaceholder(Object placeholder) {
        if(placeholder instanceof LocalizedString)
            target.setPlaceholder((LocalizedString) placeholder, getVersion());
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) placeholder, PropertyDrawExtraType.PLACEHOLDER, getVersion());
    }

    @SuppressWarnings("unused")
    public void setPattern(Object pattern) {
        if(pattern instanceof LocalizedString)
            target.setPattern((LocalizedString) pattern, getVersion());
        else {
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) pattern, PropertyDrawExtraType.PATTERN, getVersion());
        }
    }

    @SuppressWarnings("unused")
    public void setRegexp(Object regexp) {
        if(regexp instanceof LocalizedString)
            target.setRegexp((LocalizedString) regexp, getVersion());
        else {
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) regexp, PropertyDrawExtraType.REGEXP, getVersion());
        }
    }

    @SuppressWarnings("unused")
    public void setRegexpMessage(Object regexpMessage) {
        if(regexpMessage instanceof LocalizedString)
            target.setRegexpMessage((LocalizedString) regexpMessage, getVersion());
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) regexpMessage, PropertyDrawExtraType.REGEXPMESSAGE, getVersion());
    }

    @Override
    public void setFont(Object font) {
        if(font instanceof PropertyObjectEntity) {
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) font, PropertyDrawExtraType.FONT, getVersion());
        } else {
            super.setFont(font);
        }
    }

    @Override
    public void setBackground(Object background) {
        if (background instanceof PropertyObjectEntity) {
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) background, PropertyDrawExtraType.BACKGROUND, getVersion());
        } else {
            super.setBackground(background);
        }
    }

    @Override
    public void setForeground(Object foreground) {
        if (foreground instanceof PropertyObjectEntity) {
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) foreground, PropertyDrawExtraType.FOREGROUND, getVersion());
        } else {
            super.setForeground(foreground);
        }
    }

    @Override
    public void setShowIf(PropertyObjectEntity<?> showIf) {
        target.entity.setPropertyExtra(showIf, PropertyDrawExtraType.SHOWIF, getVersion());
//        super.setShowIf(showIf);
    }

    @SuppressWarnings("unused")
    public void setTooltip(Object tooltip) {
        Version version = getVersion();
        if(tooltip instanceof LocalizedString)
            target.setTooltip((LocalizedString) tooltip, version);
        else {
            if (target.getNFTooltip(version) == null)
                target.setTooltip(LocalizedString.NONAME, version);
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) tooltip, PropertyDrawExtraType.TOOLTIP, version);
        }
    }

    @SuppressWarnings("unused")
    public void setValueTooltip(Object valueTooltip) {
        Version version = getVersion();
        if(valueTooltip instanceof LocalizedString)
            target.setValueTooltip((LocalizedString) valueTooltip, version);
        else {
            if (target.getNFValueTooltip(getVersion()) == null)
                target.setValueTooltip(LocalizedString.NONAME, version);
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueTooltip, PropertyDrawExtraType.VALUETOOLTIP, version);
        }
    }

    @SuppressWarnings("unused")
    public void setWrap(boolean wrap) {
        target.setWrap(wrap, getVersion());
    }

    @SuppressWarnings("unused")
    public void setHighlightDuplicate(boolean highlightDuplicate) {
        target.setHighlightDuplicate(highlightDuplicate, getVersion());
    }

    @SuppressWarnings("unused")
    public void setWrapWordBreak(boolean wrapWordBreak) {
        target.setWrapWordBreak(wrapWordBreak, getVersion());
    }

    @SuppressWarnings("unused")
    public void setEllipsis(boolean ellipsis) {
        target.setEllipsis(ellipsis, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCollapse(boolean collapse) {
        target.setCollapse(collapse, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionWrap(boolean wrap) {
        target.setCaptionWrap(wrap, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionWrapWordBreak(boolean wrapWordBreak) {
        target.setCaptionWrapWordBreak(wrapWordBreak, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionEllipsis(boolean ellipsis) {
        target.setCaptionEllipsis(ellipsis, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionCollapse(boolean collapse) {
        target.setCaptionCollapse(collapse, getVersion());
    }

    @SuppressWarnings("unused")
    public void setClearText(boolean clearText) {
        target.setClearText(clearText, getVersion());
    }

    @SuppressWarnings("unused")
    public void setNotSelectAll(boolean notSelectAll) {
        target.setNotSelectAll(notSelectAll, getVersion());
    }

    @SuppressWarnings("unused")
    public void setAskConfirm(boolean askConfirm) {
        target.entity.setAskConfirm(askConfirm, getVersion());
    }

    @SuppressWarnings("unused")
    public void setAskConfirmMessage(LocalizedString askConfirmMessage) {
        target.entity.setAskConfirmMessage(askConfirmMessage.getSourceString(), getVersion());
    }

    @SuppressWarnings("unused")
    public void setToolbar(boolean toolbar) {
        target.setToolbar(toolbar, getVersion());
    }

    @SuppressWarnings("unused")
    public void setToolbarActions(boolean toolbarActions) {
        target.setToolbarActions(toolbarActions, getVersion());
    }

    @SuppressWarnings("unused")
    public void setNotNull(boolean notNull) {
        target.setNotNull(notNull, getVersion());
    }

    @SuppressWarnings("unused")
    public void setSelect(Object select) {
        Version version = getVersion();
        if(select instanceof LocalizedString) {
            target.entity.setCustomRenderFunction(PropertyDrawEntity.SELECT + select, version);
        } else if(select instanceof PropertyObjectEntity && ((PropertyObjectEntity<?>) select).property.isExplicitNull()) {
            target.entity.setCustomRenderFunction(PropertyDrawEntity.NOSELECT, version);
        } else {
            throw new UnsupportedOperationException("Unsupported value: " + select);
        }
    }

    @SuppressWarnings("unused")
    public void setBoxed(boolean boxed) {
        //do nothing in v7, backward compatibility
    }
}
