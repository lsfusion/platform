package lsfusion.interop.form;

// SHOW ... ACTIVATE: instead of opening a second instance, the form already open is activated. Which of the two the
// open asks for is the difference between a screen the application keeps single and a convenience the user may
// override, and the field carrying this is null when the open asks for neither
public enum FormActivateType {
    // ACTIVATE USER: the user decides - the form is reused only while duplicate forms are forbidden for them, and
    // the desktop client's Ctrl opens another instance anyway. This is what a navigator form element asks for
    USER,
    // ACTIVATE: the application decides, and neither the setting nor Ctrl changes it - what a header, a side panel
    // or a kiosk screen needs, since a second instance of it is not a thing the window can hold
    FIXED;

    public byte serialize() {
        return (byte) (ordinal() + 1);
    }

    public static FormActivateType deserialize(byte type) {
        return type == 0 ? null : values()[type - 1];
    }
}
