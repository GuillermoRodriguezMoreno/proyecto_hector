package org.iesvdm.api_rest.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.iesvdm.api_rest.domain.Invitation;
import org.iesvdm.api_rest.domain.User;
import org.iesvdm.api_rest.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class MailSenderService {

    final private String IMAGES_DIR = "img/";
    @SuppressWarnings("unused")
    final private String PDF_DIR = "pdf/";
    final private String BANNER_IMG = "banner.png";

    @Value("${spring.mail.username}")
    private String emailSenderUser;
    @Value("${app.ATTACH_PATH}")
    private String ATTACH_PATH;

    @Autowired
    private SpringTemplateEngine thymeleafTemplateEngine;
    @Autowired
    private InvitationService invitationService;
    @Autowired
    private UserService userService;
    private JavaMailSender mailSender;
    @Autowired
    public MailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // SIMPLE SEND:
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
    public void send(String from, String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
    public void sendWithInline(String from, String to, String subject,
                               String text, String contentIdBanner,
                               InputStreamSource inputStreamBanner, String contentTypeBanner) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);
        helper.addInline(contentIdBanner, inputStreamBanner, contentTypeBanner);
        mailSender.send(message);
    }
    public void sendWithAttach(String from, String to, String subject,
                               String text, String contentIdBanner,
                               InputStreamSource inputStreamBanner, String contentTypeBanner, String attachName,
                               InputStreamSource inputStreamFile) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);
        helper.addInline(contentIdBanner, inputStreamBanner, contentTypeBanner);
        helper.addAttachment(attachName, inputStreamFile);
        mailSender.send(message);
    }

    // Receives Invitation's Id and sends email notification to guest:
    @Async
    public void sendInvitation(Long invitationId) {

        Invitation targetInvitation =  invitationService.one(invitationId);

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("name", targetInvitation.getName());
        templateModel.put("names", targetInvitation.getWedding().getPartner1name() + " & " + targetInvitation.getWedding().getPartner2name());

        Context thymeleafContext = new Context();
        thymeleafContext.setVariables(templateModel);
        String htmlBody = thymeleafTemplateEngine.process("invitation.html", thymeleafContext);
        byte[] qrArr;
        //byte[] qrArr2 = new byte[0];

        try {
            qrArr = Files.readAllBytes(Paths.get(ATTACH_PATH+IMAGES_DIR+BANNER_IMG));
            //qrArr2 = Files.readAllBytes(Paths.get("fileName"));
        } catch (IOException e) {
            throw new RuntimeException("Error reading file to be attached: " + e);
        }

        try { // Use sendWithAttach to add pdf Invitation:
            this.sendWithInline(emailSenderUser,
                    targetInvitation.getEmail(),
                    "Hola " + targetInvitation.getName() + ", tienes una invitación",
                    htmlBody,
                    "banner.png", new ByteArrayResource(qrArr), "image/png"
                    //"fileName", new ByteArrayResource(qrArr2)
            );
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email: " + e);
        }
    }

    // Receives new user registered and gives back email confirmation:
    @Async
    public void sendConfirmation(Long userId) {

        try {
            User user = userService.one(userId);

            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("name", user.getUsername());

            Context thymeleafContext = new Context();
            thymeleafContext.setVariables(templateModel);
            String htmlBody = thymeleafTemplateEngine.process("confirmation.html", thymeleafContext);
            byte[] qrArr;

            try {
                qrArr = Files.readAllBytes(Paths.get(ATTACH_PATH+IMAGES_DIR+BANNER_IMG));
            } catch (IOException e) {
                throw new RuntimeException("Error reading file to be attached: " + e);
            }

            try {
                this.sendWithInline(emailSenderUser,
                        user.getEmail(),
                        "Hola " + user.getUsername() + ", bienvenido a Guestify",
                        htmlBody,
                        "banner.png", new ByteArrayResource(qrArr), "image/png"
                );
            } catch (MessagingException e) {
                throw new RuntimeException("Error sending email: " + e);
            }

        }catch(EntityNotFoundException e){
            throw new RuntimeException("Error finding user: " + e);
        }
    }

    // Receives accepted invitation's id
    @Async
    public void sendNotification(Long invitationId) {

        Invitation targetInvitation =  invitationService.one(invitationId);
        String customerEmail = invitationService.getUserEmailByInvitationId(invitationId);

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("name", targetInvitation.getWedding().getUser().getUsername());
        templateModel.put("guestName", targetInvitation.getName());

        Context thymeleafContext = new Context();
        thymeleafContext.setVariables(templateModel);
        String htmlBody = thymeleafTemplateEngine.process("notification.html", thymeleafContext);
        byte[] qrArr;

        try {
            qrArr = Files.readAllBytes(Paths.get(ATTACH_PATH+IMAGES_DIR+BANNER_IMG));
        } catch (IOException e) {
            throw new RuntimeException("Error reading file to be attached: " + e);
        }

        try {
            this.sendWithInline(emailSenderUser,
                    customerEmail,
                    targetInvitation.getName() + "'s invitation accepted!",
                    htmlBody,
                    "banner.png", new ByteArrayResource(qrArr), "image/png"
            );
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email: " + e);
        }
    }

}
