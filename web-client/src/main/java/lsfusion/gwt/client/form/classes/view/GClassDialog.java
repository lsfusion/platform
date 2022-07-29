package lsfusion.gwt.client.form.classes.view;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.DialogModalWindow;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.classes.GObjectClass;

public class GClassDialog extends DialogModalWindow {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private final boolean concreate;
    private final ClassChosenHandler classChosenHandler;

    private ClassTreePanel classPanel;

    private GObjectClass chosenClass;

    public GClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        super(false, null);

        this.concreate = concreate;
        this.classChosenHandler = classChosenHandler;

        setCaption(messages.choosingClass());

        configureLayout(baseClass, defaultClass);
    }

    private void configureLayout(GObjectClass baseClass, GObjectClass defaultClass) {
        classPanel = new ClassTreePanel(baseClass, defaultClass) {
            @Override
            public void classChosen() {
                okPressed();
            }
        };

        setBodyWidget(classPanel);

        FormButton btnOk = new FormButton(messages.ok(), FormButton.ButtonStyle.PRIMARY, event -> okPressed());
        addFooterWidget(btnOk);

        FormButton btnCancel = new FormButton(messages.cancel(), FormButton.ButtonStyle.SECONDARY, event -> chooseClass(null));
        addFooterWidget(btnCancel);
    }

    private void okPressed() {
        GObjectClass selectedClass = classPanel.getSelectedClass();
        if (selectedClass != null) {
            chooseClass(selectedClass);
        }
    }

    private void chooseClass(GObjectClass chosenClass) {
        if (chosenClass == null || !concreate || chosenClass.concreate) {
            this.chosenClass = chosenClass;
            hide();
        }
    }

    public static void showDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, ClassChosenHandler classChosenHandler) {
        new GClassDialog(baseClass, defaultClass, concreate, classChosenHandler).show();
    }

    @Override
    public void hide() {
        super.hide();
        classChosenHandler.onClassChosen(chosenClass);
    }
}
