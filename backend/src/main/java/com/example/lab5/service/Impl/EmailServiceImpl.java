package com.example.lab5.service.Impl;

import com.example.lab5.config.AsyncConfig;
import com.example.lab5.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String senderName;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled:true}") boolean enabled,
            @Value("${app.mail.from:no-reply@cardealer.vn}") String from,
            @Value("${app.mail.sender-name:Car Dealer}") String senderName
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.senderName = senderName;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    public void send(String to, String subject, String htmlBody) {
        if (!enabled) {
            log.debug("Bỏ qua gửi email (app.mail.enabled=false): {} -> {}", subject, to);
            return;
        }
        if (to == null || to.isBlank()) {
            log.debug("Bỏ qua gửi email vì khách không có địa chỉ email: {}", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, senderName);
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(buildHtml(subject, htmlBody), true);
            mailSender.send(message);
            log.info("Đã gửi email '{}' tới {}", subject, to);
        } catch (Exception exception) {
            // Không ném lỗi ra ngoài: email là kênh phụ, không được làm hỏng giao dịch.
            log.warn("Gửi email '{}' tới {} thất bại: {}", subject, to, exception.getMessage());
        }
    }

    private String buildHtml(String subject, String content) {
        String safeSubject = HtmlUtils.htmlEscape(subject, "UTF-8");
        String safeContent = HtmlUtils.htmlEscape(content, "UTF-8").replace("\n", "<br/>");
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <body style="margin:0;padding:0;background:#f6f6f6;font-family:Arial,Helvetica,sans-serif;color:#0a0a0a">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f6f6f6;padding:24px 12px">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                               style="max-width:560px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e5e7eb">
                          <tr>
                            <td style="background:#0a0a0a;padding:20px 24px;color:#ffffff">
                              <div style="font-size:20px;font-weight:700;letter-spacing:1px">CARDEALER</div>
                              <div style="font-size:12px;color:#c58e00;margin-top:4px">Đại lý xe uy tín</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:24px">
                              <h2 style="margin:0 0 12px;font-size:18px;color:#0a0a0a">%s</h2>
                              <div style="font-size:14px;line-height:1.65;color:#374151">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 24px;background:#fafafa;border-top:1px solid #e5e7eb;font-size:12px;color:#9ca3af">
                              Email tự động từ hệ thống Car Dealer. Vui lòng không trả lời trực tiếp email này.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(safeSubject, safeContent);
    }
}
