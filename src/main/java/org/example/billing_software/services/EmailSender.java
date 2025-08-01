package org.example.billing_software.services;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

public class EmailSender {
    private static final Properties cfg = new Properties();
    static {
        try (FileInputStream in = new FileInputStream("src/main/resources/config/email.properties")) {
            cfg.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load email config: " + e.getMessage());
        }
    }

    private static final String SMTP_HOST = cfg.getProperty("smtp.host");
    private static final String SMTP_PORT = cfg.getProperty("smtp.port");
    private static final String USERNAME  = cfg.getProperty("email.user");
    private static final String PASSWORD  = cfg.getProperty("email.pass");
    /**
     * Send a plain-text email with no attachment
     */
    public static void sendEmail(String to, String subject, String body) throws MessagingException {
        Message msg = prepareMessage(to, subject);
        msg.setText(body);
        Transport.send(msg);
    }

    /**
     * Send a plain-text email with one file attachment (PDF, PNG, etc)
     */
    public static void sendEmailWithAttachmentBytes(
            String to, String subject, String body,
            byte[] attachmentBytes, String filename
    ) throws MessagingException, IOException {
        Message msg = prepareMessage(to, subject);
        // text part
        MimeBodyPart text = new MimeBodyPart();
        text.setText(body);

        // attachment part
        MimeBodyPart attach = new MimeBodyPart();
        DataSource ds = new ByteArrayDataSource(
                new ByteArrayInputStream(attachmentBytes), "application/pdf");
        attach.setDataHandler(new DataHandler(ds));
        attach.setFileName(filename);

        Multipart mp = new MimeMultipart();
        mp.addBodyPart(text);
        mp.addBodyPart(attach);
        msg.setContent(mp);

        Transport.send(msg);
    }


    // shared setup for session + from/to/subject
    private static MimeMessage prepareMessage(String to, String subject) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(USERNAME));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject);
        return msg;
    }
}
