package platform.fullclient.layout;

import bibliothek.gui.dock.common.MultipleCDockableLayout;
import bibliothek.util.xml.XElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public  class FormLayout implements MultipleCDockableLayout {
        private String formSID;

        public String getFormSID() {
            return formSID;
        }

        public void setFormID(String formSID) {
            this.formSID = formSID;
        }

        public void readStream(DataInputStream in) throws IOException {
            formSID = in.readUTF();
        }

        public void readXML(XElement element) {
            formSID = element.getString();
        }

        public void writeStream(DataOutputStream out) throws IOException {
            out.writeUTF(formSID);
        }

        public void writeXML(XElement element) {
            element.setString(formSID);
        }
    }