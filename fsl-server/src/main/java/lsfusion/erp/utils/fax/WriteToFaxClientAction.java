package lsfusion.erp.utils.fax;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import org.fax4j.FaxClient;
import org.fax4j.FaxClientFactory;
import org.fax4j.FaxJob;
import org.fax4j.FaxJobStatus;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;


public class WriteToFaxClientAction implements ClientAction {

    String text;
    String faxNumber;

    public WriteToFaxClientAction(String text, String faxNumber) {
        this.text = text;
        this.faxNumber = faxNumber;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        if (text != null) {
            FaxClient faxClient = FaxClientFactory.createFaxClient();

            //create a new fax job
            FaxJob faxJob = faxClient.createFaxJob();

            File file = File.createTempFile("fax", ".txt");
            PrintWriter writer = new PrintWriter(file);
            writer.println(text);
            writer.close();

            //set fax job values
            faxJob.setFile(file);
            faxJob.setPriority(FaxJob.FaxJobPriority.HIGH_PRIORITY);
            faxJob.setTargetAddress(faxNumber);
            faxJob.setTargetName("YourName");
            faxJob.setSenderEmail("myemail@mycompany.com");
            faxJob.setSenderName("MyName");

            //submit fax job
            faxClient.submitFaxJob(faxJob);

            while (faxClient.getFaxJobStatus(faxJob) == FaxJobStatus.PENDING) {
                try {
                    Thread.sleep(100);
                    System.out.println("sending fax...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("status: " + faxClient.getFaxJobStatus(faxJob));
            file.delete();
        }
        return null;
    }
}
