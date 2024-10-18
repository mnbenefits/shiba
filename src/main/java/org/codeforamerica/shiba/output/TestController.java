package org.codeforamerica.shiba.output;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
//@Profile("test")
public class TestController {
	
	private final DocumentUploadEmailService documentUploadEmailService;
	
	public TestController(DocumentUploadEmailService documentUploadEmailService) {
		this.documentUploadEmailService = documentUploadEmailService;
	}
	
@PostMapping("/trigger-email-reminders")
public String triggerEmailReminders() {
	documentUploadEmailService.sendDocumentUploadEmailRemindersForTesting();
	return "Email reminder process triggered";
  }
}
