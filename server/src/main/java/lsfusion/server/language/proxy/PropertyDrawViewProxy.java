package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.data.time.TimeClass;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
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

    public void setPanelCaptionAfter(boolean panelCaptionAfter) {
        target.panelCaptionAfter = panelCaptionAfter;
    }

    public void setEditOnSingleClick(boolean editOnSingleClick) {
        target.editOnSingleClick = editOnSingleClick;
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
        if (target.valueSize == null) {
            target.valueSize = new Dimension(-1, prefHeight);
        } else {
            target.valueSize.height = prefHeight;
        }
    }
    public void setValueWidth(int prefWidth) {
        if (target.valueSize == null) {
            target.valueSize = new Dimension(prefWidth, -1);
        } else {
            target.valueSize.width = prefWidth;
        }
    }

    public void setCharHeight(int charHeight) {
        target.setCharHeight(charHeight);
    }

    public void setCharWidth(int charWidth) {
        target.setCharWidth(charWidth);
    }

    public void setChangeKey(KeyStroke changeKey) {
        target.changeKey = new KeyInputEvent(changeKey);
    }
    public void setChangeKeyPriority(int priority) {
        target.changeKeyPriority = priority;
    }

    public void setChangeMouse(String changeMouse) {
        target.changeMouse = new MouseInputEvent(changeMouse);
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

    //for backward compatibility
    public void setPanelCaptionAbove(boolean panelCaptionAbove) {
        target.panelCaptionVertical = panelCaptionAbove;
    }

    public void setPanelCaptionVertical(boolean panelCaptionVertical) {
        target.panelCaptionVertical = panelCaptionVertical;
    }

    //for backward compatibility
    public void setColumnKeysVertical(boolean columnKeysVertical) {
        target.panelColumnVertical = columnKeysVertical;
    }

    public void setPanelColumnVertical(boolean panelColumnVertical) {
        target.panelColumnVertical = panelColumnVertical;
    }

    public void setCaption(LocalizedString caption) {
        target.caption = caption;
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
    
    public void setNotNull(boolean notNull) {
        target.notNull = notNull;
    }
}
