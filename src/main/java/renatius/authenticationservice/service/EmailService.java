package renatius.authenticationservice.service;

public interface EmailService {

    void send(String to, String subject, String text);

}
