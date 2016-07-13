package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.IntegralClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.view.PropertyDrawView;

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

    public void setRegexp(String regexp) {
        target.regexp = regexp;
    }

    public void setRegexpMessage(String regexpMessage) {
        target.regexpMessage = regexpMessage;
    }

    public void setPattern(String pattern) {
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

    public void setCaption(String caption) {
        target.caption = caption;
    }

    public void setClearText(boolean clearText) {
        target.clearText = clearText;
    }

    public void setAskConfirm(boolean askConfirm) {
        target.entity.askConfirm = askConfirm;
    }

    public void setAskConfirmMessage(String askConfirmMessage) {
        target.entity.askConfirmMessage = askConfirmMessage;
    }
    
    public void setToolTip(String toolTip) {
        target.toolTip = toolTip;
    }
    
    public void setNotNull(boolean notNull) {
        target.notNull = notNull;
    }
}
