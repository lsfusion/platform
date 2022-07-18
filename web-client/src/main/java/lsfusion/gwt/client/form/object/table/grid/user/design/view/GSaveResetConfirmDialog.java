package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.Callback;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.FormRadioButton;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.view.MainFrame;

public class GSaveResetConfirmDialog {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    
    public boolean forAll = false;
    public boolean complete = false;
    private final boolean save;

    private DialogBoxHelper.MessageBox mb = null;

    public GSaveResetConfirmDialog(boolean save) {
        this.save = save;
    }
    
    public void show(final Callback callback) {
        DialogBoxHelper.OptionType[] options;
        Widget contents;
        FormRadioButton currentUserRB = null;
        if (!MainFrame.showDetailedInfo) {
            options = new DialogBoxHelper.OptionType[] {DialogBoxHelper.OptionType.YES, DialogBoxHelper.OptionType.NO};

            contents = new HTML(save ? messages.formGridPreferencesSureToSave() : messages.formGridPreferencesSureToReset());
        } else {
            options = new DialogBoxHelper.OptionType[] {DialogBoxHelper.OptionType.OK, DialogBoxHelper.OptionType.CANCEL};

            currentUserRB = new FormRadioButton("group", messages.formGridPreferencesForCurrentUser());
            currentUserRB.addValueChangeHandler(event -> {
                Boolean value = event.getValue();
                forAll = value == null || !value;
                complete = value == null || !value;
            });
            currentUserRB.addKeyPressHandler(event -> radioKeyPressed(event, callback));
            currentUserRB.setValue(true);

            FormRadioButton allUsersRB = new FormRadioButton("group", messages.formGridPreferencesForAllUsers());
            allUsersRB.addValueChangeHandler(event -> {
                Boolean value = event.getValue();
                forAll = value != null && value;
                complete = value == null || !value;
            });
            allUsersRB.addKeyPressHandler(event -> radioKeyPressed(event, callback));

            VerticalPanel panel = new VerticalPanel();
            panel.add(new HTML((save ? messages.formGridPreferencesSave() : messages.formGridPreferencesReset()) + ":"));
            panel.add(GwtClientUtils.createVerticalStrut(6));
            panel.add(currentUserRB);
            panel.add(allUsersRB);
            if (!save) {
                FormRadioButton allUsersCompleteRB = new FormRadioButton("group", messages.formGridPreferencesForAllUsersComplete());
                allUsersCompleteRB.addValueChangeHandler(event -> {
                    Boolean value = event.getValue();
                    forAll = value != null && value;
                    complete = value != null && value;
                });
                allUsersCompleteRB.addKeyPressHandler(event -> radioKeyPressed(event, callback));
                panel.add(allUsersCompleteRB);
            }
            contents = panel;
        }

        
        mb = DialogBoxHelper.showConfirmBox(save ? messages.formGridPreferencesSaving() : messages.formGridPreferencesResetting(), contents, options, chosenOption -> {
            if (chosenOption.asInteger() == 0) {
                callback.onSuccess();
            } else {
                callback.onFailure();
            }
        });
        if (currentUserRB != null) {
            currentUserRB.setFocus(true);
        }
    }
    
    private void radioKeyPressed(KeyPressEvent event, Callback callback) {
        if (GKeyStroke.isEnterKeyEvent(event.getNativeEvent())) {
            if (mb != null) {
                mb.hide();
            }
            callback.onSuccess();
        }
    }
}
