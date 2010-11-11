package platform.fullclient.layout;

import bibliothek.gui.dock.common.MultipleCDockableLayout;
import bibliothek.util.xml.XElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public  class FormLayout implements MultipleCDockableLayout {
        private Integer formID;

        public Integer getFormID() {
            return formID;
        }

        public void setFormID(Integer formID) {
            this.formID = formID;
        }

        public void readStream(DataInputStream in) throws IOException {
            formID = in.readInt();
        }

        public void readXML(XElement element) {
            formID = element.getInt();
        }

        public void writeStream(DataOutputStream out) throws IOException {
            out.writeInt(formID);
        }

        public void writeXML(XElement element) {
            element.setInt(formID);
        }
    }