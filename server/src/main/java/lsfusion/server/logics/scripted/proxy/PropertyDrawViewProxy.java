package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.IntegralClass;
import lsfusion.server.classes.TimeClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.PropertyUtils;
import lsfusion.server.logics.i18n.LocalizedString;

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
        Type type = target.getType();
        if (type instanceof IntegralClass) {
            target.format = new DecimalFormat(pattern);
        } else if (type instanceof DateClass || type instanceof TimeClass || type instanceof DateTimeClass) {
            target.format = new SimpleDateFormat(pattern);
        }
        target.pattern = pattern;
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
        target.defaultCompare = PropertyUtils.stringToCompare(defaultCompare);
    }

    public void setValueSize(Dimension size) {
        target.setMinimumValueSize(size);
    }
    public void setValueHeight(int prefHeight) {
        if (target.minimumValueSize == null) {
            target.minimumValueSize = new Dimension(-1, prefHeight);
        } else {
            target.minimumValueSize.height = prefHeight;
        }
    }
    public void setValueWidth(int prefWidth) {
        if (target.minimumValueSize == null) {
            target.minimumValueSize = new Dimension(prefWidth, -1);
        } else {
            target.minimumValueSize.width = prefWidth;
        }
    }
    public void setCharWidth(int charWidth) {
        target.setMinimumCharWidth(charWidth);
    }

    public void setChangeKey(KeyStroke editKey) {
        target.changeKey = editKey;
    }

    public void setShowChangeKey(boolean showEditKey) {
        target.showChangeKey = showEditKey;
    }

    public void setFocusable(Boolean focusable) {
        target.focusable = focusable;
    }

    public void setPanelCaptionAbove(boolean panelCaptionAbove) {
        target.panelCaptionAbove = panelCaptionAbove;
    }

    public void setCaption(LocalizedString caption) {
        target.caption = caption;
    }

    public void setClearText(boolean clearText) {
        target.clearText = clearText;
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
