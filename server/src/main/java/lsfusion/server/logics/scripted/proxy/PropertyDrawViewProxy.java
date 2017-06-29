package lsfusion.server.logics.scripted.proxy;

import lsfusion.interop.Compare;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.IntegralClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.i18n.LocalizedString;

import javax.swing.*;
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
        if(type instanceof IntegralClass) {
            target.format = new DecimalFormat(pattern);
        } else if(type instanceof DateClass) {
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
        switch (defaultCompare) {
            case "EQUALS":
                target.defaultCompare = Compare.EQUALS;
                break;
            case "GREATER":
                target.defaultCompare = Compare.GREATER;
                break;
            case "LESS":
                target.defaultCompare = Compare.LESS;
                break;
            case "GREATER_EQUALS":
                target.defaultCompare = Compare.GREATER_EQUALS;
                break;
            case "LESS_EQUALS":
                target.defaultCompare = Compare.LESS_EQUALS;
                break;
            case "NOT_EQUALS":
                target.defaultCompare = Compare.NOT_EQUALS;
                break;
            case "START_WITH":
                target.defaultCompare = Compare.START_WITH;
                break;
            case "CONTAINS":
                target.defaultCompare = Compare.CONTAINS;
                break;
            case "ENDS_WITH":
                target.defaultCompare = Compare.ENDS_WITH;
                break;
            case "LIKE":
                target.defaultCompare = Compare.LIKE;
                break;
            case "INARRAY":
                target.defaultCompare = Compare.INARRAY;
                break;
        }
    }

    public void setMinimumCharWidth(int minimumCharWidth) {
        target.setMinimumCharWidth(minimumCharWidth);
    }

    public void setMaximumCharWidth(int maximumCharWidth) {
        target.setMaximumCharWidth(maximumCharWidth);
    }

    public void setPreferredCharWidth(int preferredCharWidth) {
        target.setPreferredCharWidth(preferredCharWidth);
    }

    public void setEditKey(KeyStroke editKey) {
        target.editKey = editKey;
    }

    public void setShowEditKey(boolean showEditKey) {
        target.showEditKey = showEditKey;
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
