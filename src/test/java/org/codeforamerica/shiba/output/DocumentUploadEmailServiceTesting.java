
	package org.codeforamerica.shiba.output;
	import static org.codeforamerica.shiba.Program.CASH;
	import static org.codeforamerica.shiba.Program.SNAP;
    import static org.junit.jupiter.api.Assertions.assertEquals;
     import static org.mockito.ArgumentMatchers.eq;
	import static org.mockito.Mockito.times;
	import static org.mockito.Mockito.verify;

	import java.time.ZonedDateTime;
	import java.util.List;
	import java.util.Map;
	import org.codeforamerica.shiba.County;
	import org.codeforamerica.shiba.application.Application;
	import org.codeforamerica.shiba.application.ApplicationRepository;
	import org.codeforamerica.shiba.application.FlowType;
	import org.codeforamerica.shiba.application.Status;
	import org.codeforamerica.shiba.pages.data.ApplicationData;
	import org.codeforamerica.shiba.pages.emails.EmailClient;
import org.codeforamerica.shiba.pages.rest.CommunicationClient;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
	import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.boot.test.context.SpringBootTest;
	import org.springframework.boot.test.mock.mockito.MockBean;
	import org.springframework.http.MediaType;
	import org.springframework.mock.web.MockMultipartFile;
	import org.springframework.test.context.ActiveProfiles;

	import com.google.gson.JsonObject;

	@SpringBootTest
	@ActiveProfiles("test")
	public class DocumentUploadEmailServiceTesting { 

	  private final String CLIENT_EMAIL = "client@example.com";

	  @Autowired
	  private DocumentUploadEmailService documentUploadEmailService;

	  @MockBean
	  private CommunicationClient commHubEmailSendingClient;
	  
	  @Captor
	  private ArgumentCaptor<JsonObject> jsonCaptor;

	  @Test
	  void sendDocumentUploadEmailsTest() {
	 
	    documentUploadEmailService.sendDocumentUploadEmailReminders();
	    
	    //use argumentcaptor to capture the JsonObject sent to sendEmailDataToCommhub() in CommunicationClient.
	    ArgumentCaptor<JsonObject> jsonCaptor = ArgumentCaptor.forClass(JsonObject.class);
	    verify(commHubEmailSendingClient, times(1)).sendEmailDataToCommhub(jsonCaptor.capture());

	    JsonObject emailJson = jsonCaptor.getValue(); 

	    assertEquals("[Action Required] Upload Documents To Your MNbenefits Application", emailJson.get("subject").getAsString());
	    assertEquals("sender@email.org", emailJson.get("senderEmail").getAsString());
	    assertEquals(CLIENT_EMAIL, emailJson.get("recepientEmail").getAsString());
	    assertEquals("<html><body>Remember to upload documents on <a href=\"https://mnbenefits.mn.gov/?utm_medium=confirmationemail#later-docs-upload\" target=\"_blank\" rel=\"noopener noreferrer\">MNbenefits.mn.gov</a> to support your MN Benefits application. You can use your phone to take or upload pictures, or use your computer to upload documents.<br>If you have them, you should upload the following documents:<br><ul><li><strong>Proof of Income:</strong> A document with employer and employee names and your total pre-tax income from the last 30 days (or total hours worked and rate of pay). Example: Paystubs</li></ul>If you have already uploaded these documents, you can ignore this reminder.</body></html>", 
	                 emailJson.get("emailContent").getAsString());
	    assertEquals("qrt386", emailJson.get("applicationId").getAsString());
	  }
	 
	}


