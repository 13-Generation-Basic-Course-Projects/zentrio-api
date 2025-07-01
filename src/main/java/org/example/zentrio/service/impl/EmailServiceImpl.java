package org.example.zentrio.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.zentrio.enums.RoleRequest;
import org.example.zentrio.exception.BadRequestException;
import org.example.zentrio.model.AppUser;
import org.example.zentrio.repository.AppUserRepository;
import org.example.zentrio.service.EmailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final SpringTemplateEngine templateEngine;
    private final JavaMailSender javaMailSender;
    private final AppUserRepository appUserRepository;

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
    public void sendInvitations(UUID workspaceId, UUID boardId, String email, RoleRequest role) {
        log.info("Sending invitation to email: {} for workspace: {}, board: {}, role: {}", email, workspaceId, boardId, role);

        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BadRequestException("Invalid email address: " + email);
        }

        if (role == null) {
            throw new BadRequestException("Role cannot be null or empty");
        }

        try {
            // Prepare the email template context
            Context context = new Context();
            context.setVariable("title", "You're Invited to Join a Project!");
            context.setVariable("message",
                    "You’ve been invited to join a project board. Click the button below to accept the invitation.");

            // Generate the frontend invitation URL
            String acceptInvitationUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/boards/" + boardId + "/workspaces/" + workspaceId + "/invitations/accept")
                    .queryParam("email", email)
                    .queryParam("roleRequest", role.name())
                    .toUriString();

            AppUser user = appUserRepository.getUserByEmail(email);
            String loginInvitationUrl = "http://localhost:3000/login";

            context.setVariable("inviteLink", acceptInvitationUrl);

            log.info("Generated invitation URL: {}", acceptInvitationUrl);

            // Process the Thymeleaf template
            String processedHtml = templateEngine.process("invite-member", context);

            // Create and send the email
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            mimeMessageHelper.setSubject("You're Invited to Join Zentrio!");
            mimeMessageHelper.setText(processedHtml, true);
            mimeMessageHelper.setTo(email);

            javaMailSender.send(mimeMessage);
            log.info("Invitation email sent to {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send invitation email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send invitation email to " + email, e);
        } catch (Exception e) {
            log.error("Unexpected error while sending invitation to {}: {}", email, e.getMessage());
            throw new RuntimeException("Unexpected error while sending invitation to " + email, e);
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
