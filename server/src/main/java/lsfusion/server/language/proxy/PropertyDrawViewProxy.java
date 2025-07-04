package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.event.InputBindingEvent;
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

    public void setAutoSize(boolean autoSize) {
        if(target.valueWidth == null || target.valueWidth < 0)
            target.valueWidth = autoSize ? -1 : -2;
        if(target.valueHeight == null || target.valueHeight < 0)
            target.valueHeight = autoSize ? -1 : -2;
    }

    public void setBoxed(boolean boxed) {
        target.boxed = boxed;
    }

    public void setChangeOnSingleClick(boolean changeOnSingleClick) {
        target.changeOnSingleClick = changeOnSingleClick;
    }

    public void setHide(boolean hide) {
        target.entity.hide = hide;
    }

    public void setMaxValue(long maxValue) {
        target.maxValue = maxValue;
    }

    public void setEchoSymbols(boolean echoSymbols) {
        target.echoSymbols = echoSymbols;
    }

    public void setNoSort(boolean noSort) {
        target.noSort = noSort;
    }

    public void setDefaultCompare(String defaultCompare) {
        target.defaultCompare = ActionOrPropertyUtils.stringToCompare(defaultCompare);
    }

    public void setValueSize(Dimension size) {
        target.setValueSize(size);
    }
    public void setValueHeight(int prefHeight) {
        target.valueHeight = prefHeight;
    }
    public void setValueWidth(int prefWidth) {
        target.valueWidth = prefWidth;
    }

    public void setCaptionHeight(int prefHeight) {
        target.captionHeight = prefHeight;
    }
    public void setCaptionCharHeight(int prefHeight) {
        target.captionCharHeight = prefHeight;
    }
    public void setCaptionWidth(int prefWidth) {
        target.captionWidth = prefWidth;
    }

    public void setCharHeight(int charHeight) {
        target.setCharHeight(charHeight);
    }

    public void setCharWidth(int charWidth) {
        target.setCharWidth(charWidth);
    }
    public void setValueFlex(boolean flex) {
        target.setValueFlex(flex);
    }

    public void setChangeKey(Object changeKey) {
        if(changeKey instanceof LocalizedString) {
            target.changeKey = KeyStrokeConverter.parseInputBindingEvent(changeKey.toString(), false);
        } else {
            if (target.changeKey == null) {
                //dumb value will be replaced with a dynamic one
                target.changeKey = InputBindingEvent.dumb;
            }
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) changeKey, PropertyDrawExtraType.CHANGEKEY, getVersion());
        }
    }

    //deprecated
    public void setChangeKeyPriority(int priority) {
        if(target.changeKey != null)
            target.changeKey.priority = priority;
    }

    public void setShowChangeKey(boolean showChangeKey) {
        target.showChangeKey = showChangeKey;
    }

    public void setChangeMouse(Object changeMouse) {
        if(changeMouse instanceof LocalizedString) {
            target.changeMouse = KeyStrokeConverter.parseInputBindingEvent(changeMouse.toString(), true);
        } else {
            if (target.changeMouse == null) {
                //dumb value will be replaced with a dynamic one.
                target.changeMouse = InputBindingEvent.dumb;
            }
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) changeMouse, PropertyDrawExtraType.CHANGEMOUSE, getVersion());
        }
    }

    //deprecated
    public void setChangeMousePriority(int priority) {
        if(target.changeMouse != null)
        target.changeMouse.priority = priority;
    }

    public void setShowChangeMouse(boolean showChangeMouse) {
        target.showChangeMouse = showChangeMouse;
    }

    public void setFocusable(Boolean focusable) {
        target.focusable = focusable;
    }

    public void setInline(Boolean inline) {
        target.inline = inline;
    }

    public void setPanelColumnVertical(boolean panelColumnVertical) {
        target.panelColumnVertical = panelColumnVertical;
    }

    public void setClass(Object elementClass) {
        super.setClass(elementClass);
        if(elementClass instanceof PropertyObjectEntity)
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) elementClass, PropertyDrawExtraType.GRIDELEMENTCLASS, getVersion());
    }

    public void setValueClass(Object valueClass) {
        if(valueClass instanceof LocalizedString)
            target.valueElementClass = ((LocalizedString) valueClass).getSourceString();
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueClass, PropertyDrawExtraType.VALUEELEMENTCLASS, getVersion());
    }

    public void setCaptionClass(Object valueClass) {
        if(valueClass instanceof LocalizedString)
            target.captionElementClass = ((LocalizedString) valueClass).getSourceString();
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueClass, PropertyDrawExtraType.CAPTIONELEMENTCLASS, getVersion());
    }

    public void setCaption(Object caption) {
        if(caption instanceof LocalizedString)
            target.caption = (LocalizedString) caption;
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) caption, PropertyDrawExtraType.CAPTION, getVersion());
    }

    public void setTag(LocalizedString tag) {
        target.tag = tag.getSourceString();
    }

    public void setInputType(LocalizedString inputType) {
        target.inputType = inputType.getSourceString();
    }

    public void setImagePath(LocalizedString image) {
        setImage(image);
    }

    public void setImage(Object image) {
        if(image instanceof LocalizedString)
            target.setImage(((LocalizedString) image).getSourceString());
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) image, PropertyDrawExtraType.IMAGE, getVersion());
    }

    public void setComment(Object comment) {
        if(comment instanceof LocalizedString)
            target.comment = (LocalizedString) comment;
        else {
            if (target.comment == null)
                target.comment = LocalizedString.NONAME;
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) comment, PropertyDrawExtraType.COMMENT, getVersion());
        }
    }

    public void setCommentClass(Object valueClass) {
        if(valueClass instanceof LocalizedString)
            target.commentElementClass = ((LocalizedString) valueClass).getSourceString();
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueClass, PropertyDrawExtraType.COMMENTELEMENTCLASS, getVersion());
    }

    public void setPanelCommentVertical(boolean panelCommentVertical) {
        target.panelCommentVertical = panelCommentVertical;
    }

    public void setPanelCommentFirst(boolean panelCommentFirst) {
        target.panelCommentFirst = panelCommentFirst;
    }

    public void setPanelCommentAlignment(FlexAlignment panelCommentAlignment) {
        target.panelCommentAlignment = panelCommentAlignment;
    }

    public void setPlaceholder(Object placeholder) {
        if(placeholder instanceof LocalizedString)
            target.placeholder = (LocalizedString) placeholder;
        else
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) placeholder, PropertyDrawExtraType.PLACEHOLDER, getVersion());
    }

    public void setPattern(Object pattern) {
        if(pattern instanceof LocalizedString)
            target.pattern = (LocalizedString) pattern;
        else {
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) pattern, PropertyDrawExtraType.PATTERN, getVersion());
        }
    }

    public void setRegexp(Object regexp) {
        if(regexp instanceof LocalizedString)
            target.regexp = (LocalizedString) regexp;
        else {
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) regexp, PropertyDrawExtraType.REGEXP, getVersion());
        }
    }

    public void setRegexpMessage(Object regexpMessage) {
        if(regexpMessage instanceof LocalizedString)
            target.regexpMessage = (LocalizedString) regexpMessage;
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

    public void setTooltip(Object tooltip) {
        if(tooltip instanceof LocalizedString)
            target.tooltip = (LocalizedString) tooltip;
        else {
            if (target.tooltip == null)
                target.tooltip = LocalizedString.NONAME;
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) tooltip, PropertyDrawExtraType.TOOLTIP, getVersion());
        }
    }

    public void setValueTooltip(Object valueTooltip) {
        if(valueTooltip instanceof LocalizedString)
            target.valueTooltip = (LocalizedString) valueTooltip;
        else {
            if (target.valueTooltip == null)
                target.valueTooltip = LocalizedString.NONAME;
            target.entity.setPropertyExtra((PropertyObjectEntity<?>) valueTooltip, PropertyDrawExtraType.VALUETOOLTIP, getVersion());
        }
    }

    //deprecated
    public void setValueAlignment(FlexAlignment valueAlignment) {
        setValueAlignmentHorz(valueAlignment);
    }

    public void setValueAlignmentHorz(FlexAlignment valueAlignmentHorz) {
        target.valueAlignmentHorz = valueAlignmentHorz;
    }

    public void setValueAlignmentVert(FlexAlignment valueAlignmentVert) {
        target.valueAlignmentVert = valueAlignmentVert;
    }

    private String flexAlignmentToString(FlexAlignment flexAlignment) {
        String result = null;
        if(flexAlignment != null) {
            switch (flexAlignment) {
                case START:
                    result = "start";
                    break;
                case CENTER:
                    result = "center";
                    break;
                case STRETCH:
                    result = "stretch";
                    break;
                case END:
                    result = "end";
            }
        }
        return result;
    }

    public void setPanelCustom(Boolean panelCustom) {
        target.panelCustom = panelCustom;
    }

    public void setValueOverflowHorz(String valueOverflowHorz) {
        target.valueOverflowHorz = valueOverflowHorz;
    }

    public void setValueOverflowVert(String valueOverflowVert) {
        target.valueOverflowVert = valueOverflowVert;
    }

    public void setValueShrinkHorz(Boolean valueShrinkHorz) {
        target.valueShrinkHorz = valueShrinkHorz;
    }

    public void setValueShrinkVert(Boolean valueShrinkVert) {
        target.valueShrinkVert = valueShrinkVert;
    }

    public void setWrap(boolean wrap) {
        target.wrap = wrap;
    }

    public void setHighlightDuplicate(boolean highlightDuplicate) {
        target.highlightDuplicate = highlightDuplicate;
    }

    public void setWrapWordBreak(boolean wrapWordBreak) {
        target.wrapWordBreak = wrapWordBreak;
    }

    public void setEllipsis(boolean ellipsis) {
        target.ellipsis = ellipsis;
    }

    public void setCollapse(boolean collapse) {
        target.collapse = collapse;
    }

    public void setCaptionWrap(boolean wrap) {
        target.captionWrap = wrap;
    }

    public void setCaptionWrapWordBreak(boolean wrapWordBreak) {
        target.captionWrapWordBreak = wrapWordBreak;
    }

    public void setCaptionEllipsis(boolean ellipsis) {
        target.captionEllipsis = ellipsis;
    }

    public void setCaptionCollapse(boolean collapse) {
        target.captionCollapse = collapse;
    }

    public void setClearText(boolean clearText) {
        target.clearText = clearText;
    }

    public void setNotSelectAll(boolean notSelectAll) {
        target.notSelectAll = notSelectAll;
    }

    public void setAskConfirm(boolean askConfirm) {
        target.entity.askConfirm = askConfirm;
    }

    public void setAskConfirmMessage(LocalizedString askConfirmMessage) {
        target.entity.askConfirmMessage = askConfirmMessage.getSourceString();
    }

    public void setToolbar(boolean toolbar) {
        target.toolbar = toolbar;
    }

    public void setToolbarActions(boolean toolbarActions) {
        target.toolbarActions = toolbarActions;
    }

    public void setNotNull(boolean notNull) {
        target.notNull = notNull;
    }

    public void setSelect(Object select) {
        if(select instanceof LocalizedString) {
            target.entity.customRenderFunction = PropertyDrawEntity.SELECT + select;
        } else if(select instanceof PropertyObjectEntity && ((PropertyObjectEntity<?>) select).property.isExplicitNull()) {
            target.entity.customRenderFunction = PropertyDrawEntity.NOSELECT;
        } else {
            throw new UnsupportedOperationException("Unsupported value: " + select);
        }
    }
}
