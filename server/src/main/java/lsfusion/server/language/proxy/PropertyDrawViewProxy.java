package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.data.time.TimeClass;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrPropertyUtils;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class PropertyDrawViewProxy extends ComponentViewProxy<PropertyDrawView> {

    public PropertyDrawViewProxy(PropertyDrawView target) {
        super(target);
    }

    public void setAutoSize(boolean autoSize) {
        target.autoSize = autoSize;
    }

    public void setBoxed(boolean boxed) {
        target.boxed = boxed;
    }

    public void setPanelCaptionVertical(boolean panelCaptionVertical) {
        target.panelCaptionVertical = panelCaptionVertical;
    }
    
    public void setPanelCaptionLast(boolean panelCaptionLast) {
        target.panelCaptionLast = panelCaptionLast;
    }

    public void setPanelCaptionAlignment(FlexAlignment panelCaptionAlignment) {
        target.panelCaptionAlignment = panelCaptionAlignment;
    }

    //for backward compatibility
    public void setEditOnSingleClick(boolean editOnSingleClick) {
        target.changeOnSingleClick = editOnSingleClick;
    }

    public void setChangeOnSingleClick(boolean changeOnSingleClick) {
        target.changeOnSingleClick = changeOnSingleClick;
    }

    public void setHide(boolean hide) {
        target.hide = hide;
    }

    public void setRegexp(LocalizedString regexp) {
        target.regexp = regexp.getSourceString();
    }

    public void setRegexpMessage(LocalizedString regexpMessage) {
        target.regexpMessage = regexpMessage.getSourceString();
    }

    public void setPattern(LocalizedString lPattern) {
        String pattern = lPattern.getSourceString();
        if(target.isProperty()) {
            Type type = target.getType();
            if (type instanceof IntegralClass) {
                target.format = new DecimalFormat(pattern);
            } else if (type instanceof DateClass || type instanceof TimeClass || type instanceof DateTimeClass) {
                target.format = new SimpleDateFormat(pattern);
            }
        }
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

    public void setChangeKey(ScriptingLogicsModule.KeyStrokeOptions changeKey) {
        target.changeKey = new KeyInputEvent(KeyStroke.getKeyStroke(changeKey.keyStroke), changeKey.bindingModesMap);
        target.changeKeyPriority = changeKey.priority;
    }
    public void setChangeKeyPriority(int priority) {
        target.changeKeyPriority = priority;
    }

    public void setChangeMouse(ScriptingLogicsModule.KeyStrokeOptions changeMouse) {
        target.changeMouse = new MouseInputEvent(changeMouse.keyStroke, changeMouse.bindingModesMap);
        target.changeMousePriority = changeMouse.priority;
    }
    public void setChangeMousePriority(int priority) {
        target.changeMousePriority = priority;
    }

    public void setShowChangeKey(boolean showChangeKey) {
        target.showChangeKey = showChangeKey;
    }

    public void setFocusable(Boolean focusable) {
        target.focusable = focusable;
    }

    public void setPanelColumnVertical(boolean panelColumnVertical) {
        target.panelColumnVertical = panelColumnVertical;
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

    public void setValueAlignment(FlexAlignment valueAlignment) {
        target.valueAlignment = valueAlignment;
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
    
    public void setToolTip(LocalizedString toolTip) {
        target.toolTip = toolTip.getSourceString();
    }

    public void setToolbar(boolean toolbar) {
        target.toolbar = toolbar;
    }

    public void setNotNull(boolean notNull) {
        target.notNull = notNull;
    }
}
