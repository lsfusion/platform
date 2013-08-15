package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.PropertyDrawView;

import javax.swing.*;

public class PropertyDrawViewProxy extends ComponentViewProxy<PropertyDrawView> {

    public PropertyDrawViewProxy(PropertyDrawView target) {
        super(target);
    }

    public void setAutoHide(boolean autoHide) {
        target.autoHide = autoHide;
    }

    public void setShowTableFirst(boolean showTableFirst) {
        target.showTableFirst = showTableFirst;
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

    public void setEchoSymbols(boolean echoSymbols) {
        target.echoSymbols = echoSymbols;
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

    public void setPanelLabelAbove(boolean panelLabelAbove) {
        target.panelLabelAbove = panelLabelAbove;
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
}
