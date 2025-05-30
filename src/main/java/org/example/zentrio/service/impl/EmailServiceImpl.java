package org.example.zentrio.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.zentrio.enums.RoleRequest;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.service.EmailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final SpringTemplateEngine templateEngine;
    private final JavaMailSender javaMailSender;

    @Override
    public void sendDynamicEmail(String toEmail, String templateName, String subject, Map<String, Object> variables) {

        if (!toEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BadRequestException("Invalid email address");
        }

        Context context = new Context();
        context.setVariables(variables);  // set all variables dynamically

        String htmlContent = templateEngine.process(templateName, context); // uses the given HTML file

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }


    @Override
    public void sendInvitations(UUID boardId, String email, RoleRequest role) {

       log.info("send invitations to email {}", email);

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BadRequestException("Invalid email address");
        }

        if (role == null) {
            throw new BadRequestException("Role required can't be null or empty");
        }

        try {
            // Prepare the email template context with dynamic values
            Context context = new Context();
            context.setVariable("title", "You're Invited!");  // Title for the email
            context.setVariable("message",
                    "We would like to invite you to join our platform. Please click the button below to accept the invitation.");  // Message content

            String acceptInvitationUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/boards/" + boardId + "/invitations/accept")
                            .queryParam("email",email)
                            .queryParam("role",role.name())
                            .toUriString();

            log.info("accept invitation {}", acceptInvitationUrl);

            context.setVariable("inviteLink", "https://www.youtube.com/");  // Invitation link (replace with actual URL)

            // Process the Thymeleaf template 'invite-member.html' (replace with the correct template name)
            String processedHtml = templateEngine.process("invite-member", context);  // Template name

            // Create the MimeMessage for sending the email
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setSubject("You're Invited to Join Zentrio!");  // Email subject
            mimeMessageHelper.setText(processedHtml, true);  // true means it will be sent as HTML content
            mimeMessageHelper.setTo(email);  // The recipient email

            // Send the email using JavaMailSender
            javaMailSender.send(mimeMessage);

            System.out.println("Invitation email sent to " + email);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send invitation email to " + email, e);
        }
    }



    @SneakyThrows
    @Override
    public void sendEmail(String otpCode, String email) {

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BadRequestException("Invalid email address");
        }

        try {
            Context context = new Context();
            context.setVariable("otp", otpCode);
            System.out.println("otpCode: " + otpCode);
            String process = templateEngine.process("index", context);
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setSubject("Verify your email with otp");
            mimeMessageHelper.setText(process, true);
            mimeMessageHelper.setTo(email);
            javaMailSender.send(mimeMessage);
        } catch (BadRequestException e) {
            throw new BadRequestException("Failed to send email");
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email due to messaging error.", e);
        }

    }
}
