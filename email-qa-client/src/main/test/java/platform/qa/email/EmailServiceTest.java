package platform.qa.email;

import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import platform.qa.email.service.EmailService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmailServiceTest {
    EmailService emailService = mock(EmailService.class);
    ValidatableResponse mailBoxListByUserResponse;

    @Test
    public void getMailboxListByUserTest() {
        when(emailService.getAllUserMails(anyString())).thenReturn(mailBoxListByUserResponse);

        var mailboxListByUser = emailService.getAllUserMails("test");

        assertEquals(mailBoxListByUserResponse, mailboxListByUser);
    }
}
