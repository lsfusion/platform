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
import java.util.TimeZone;

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
        } else if(type instanceof DateClass || type instanceof TimeClass || type instanceof DateTimeClass) {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            target.format = format;
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

    public void setMinimumCharWidth(int minimumCharWidth) {
        target.setMinimumCharWidth(minimumCharWidth);
    }

    public void setMaximumCharWidth(int maximumCharWidth) {
        target.setMaximumCharWidth(maximumCharWidth);
    }

    public void setPreferredCharWidth(int preferredCharWidth) {
        target.setPreferredCharWidth(preferredCharWidth);
    }

    public void setMinimumValueSize(Dimension minimumValueSize) {
        target.setMinimumValueSize(minimumValueSize);
    }
    public void setMaximumValueSize(Dimension maximumValueSize) {
        target.setMaximumValueSize(maximumValueSize);
    }
    public void setPreferredValueSize(Dimension preferredValueSize) {
        target.setPreferredValueSize(preferredValueSize);
    }

    public void setMinimumValueHeight(int minHeight) {
        if (target.minimumValueSize == null) {
            target.minimumValueSize = new Dimension(-1, minHeight);
        } else {
            target.minimumValueSize.height = minHeight;
        }
    }

    public void setMinimumValueWidth(int minWidth) {
        if (target.minimumValueSize == null) {
            target.minimumValueSize = new Dimension(minWidth, -1);
        } else {
            target.minimumValueSize.width = minWidth;
        }
    }

    public void setMaximumValueHeight(int maxHeight) {
        if (target.maximumValueSize == null) {
            target.maximumValueSize = new Dimension(-1, maxHeight);
        } else {
            target.maximumValueSize.height = maxHeight;
        }
    }

    public void setMaximumValueWidth(int maxWidth) {
        if (target.maximumValueSize == null) {
            target.maximumValueSize = new Dimension(maxWidth, -1);
        } else {
            target.maximumValueSize.width = maxWidth;
        }
    }

    public void setPreferredValueHeight(int maxHeight) {
        if (target.preferredValueSize == null) {
            target.preferredValueSize = new Dimension(-1, maxHeight);
        } else {
            target.preferredValueSize.height = maxHeight;
        }
    }

    public void setPreferredValueWidth(int maxWidth) {
        if (target.preferredValueSize == null) {
            target.preferredValueSize = new Dimension(maxWidth, -1);
        } else {
            target.preferredValueSize.width = maxWidth;
        }
    }

    public void setFixedValueSize(Dimension size) {
        setMinimumValueSize(size);
        setMaximumValueSize(size);
        setPreferredValueSize(size);
    }

    public void setFixedValueHeight(int height) {
        setMinimumValueHeight(height);
        setMaximumValueHeight(height);
        setPreferredValueHeight(height);
    }

    public void setFixedValueWidth(int width) {
        setMinimumValueWidth(width);
        setMaximumValueWidth(width);
        setPreferredValueWidth(width);
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
