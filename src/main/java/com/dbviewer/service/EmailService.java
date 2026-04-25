package com.dbviewer.service;

import com.dbviewer.model.EmailRequest;
import com.dbviewer.model.PlaceholderRequest;
import com.dbviewer.model.PlaceholderRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // Optional — only present when spring.mail.* properties are configured
    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final ExecutionRecordService executionRecordService;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.mock:true}")
    private boolean mockMode;

    // ── Send (or mock-send) performance report ────────────────────────────────
    public String sendPerformanceReport(EmailRequest request) throws Exception {
        PlaceholderRequest pr = new PlaceholderRequest();
        pr.setDate1(request.getDate1());
        pr.setDate2(request.getDate2());
        pr.setDate3(request.getDate3());
        List<PlaceholderRow> rows = executionRecordService.buildPlaceholderTable(pr);

        String subject = (request.getSubject() != null && !request.getSubject().isBlank())
                ? request.getSubject() : "Mozart Performance Result";

        String htmlBody = buildHtmlEmail(request, rows, subject);

        List<String> validEmails = request.getToEmails().stream()
                .filter(e -> e != null && !e.isBlank())
                .toList();

        // ── Mock mode: just log to console ───────────────────────────────────
        if (mockMode || mailSender == null) {
            log.info("========== MOCK EMAIL (not actually sent) ==========");
            log.info("Subject : {}", subject);
            log.info("From    : {} <{}>", fromName, fromEmail);
            log.info("To      : {}", validEmails);
            log.info("Body    : [HTML — {} chars]", htmlBody.length());
            log.info("====================================================");
            return "Mock mode — email logged to console for " + validEmails.size() + " recipient(s). "
                    + "Set app.mail.mock=false and configure SMTP to send real emails.";
        }

        // ── Real mode: send via SMTP ──────────────────────────────────────────
        for (String to : validEmails) {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Report sent to: {}", to);
        }
        return "Report sent successfully to " + validEmails.size() + " recipient(s).";
    }

    // ── HTML email builder ────────────────────────────────────────────────────
    private String buildHtmlEmail(EmailRequest req, List<PlaceholderRow> rows, String subject) {
        String sentAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"));

        String label1 = safe(req.getLabel1(), "1st Column");
        String label2 = safe(req.getLabel2(), "2nd Column");
        String label3 = safe(req.getLabel3(), "3rd Column");

        int[][] groups = {
                {0,  4,  10000},
                {4,  8,  100000},
                {8,  12, 500000},
                {12, 16, 1000000}
        };

        StringBuilder tableRows = new StringBuilder();
        for (int[] g : groups) {
            int start = g[0], end = g[1], jobs = g[2];
            String jobsLabel = formatJobs(jobs);
            for (int i = start; i < end; i++) {
                PlaceholderRow row = rows.get(i);
                String imprStyle = getImprStyle(row.getImprovement());
                String bottomBorder = (i == end - 1) ? "border-bottom:2px solid #c8c7c0;" : "";
                if (i == start) {
                    tableRows.append("<tr>")
                            .append("<td rowspan='4' style='background:#f7f7f5;font-weight:500;font-size:12px;")
                            .append("text-align:center;vertical-align:middle;padding:10px;")
                            .append("border:1px solid #e0dfd8;border-bottom:2px solid #c8c7c0;'>")
                            .append(jobsLabel).append("</td>");
                } else {
                    tableRows.append("<tr>");
                }
                tableRows
                        .append(td(String.valueOf(row.getNodes()), bottomBorder))
                        .append(td(row.getDateOf3rdCol(), bottomBorder))
                        .append(td(row.getDateOf2ndCol(), bottomBorder))
                        .append(td(row.getDateOf1stCol(), bottomBorder))
                        .append(tdStyled(row.getImprovement(), imprStyle + bottomBorder))
                        .append("</tr>\n");
            }
        }

        String col3Legend = req.getDate3() != null && !req.getDate3().isBlank()
                ? "<td><span style='display:inline-block;background:#FAEEDA;color:#633806;font-size:11px;" +
                  "font-weight:500;padding:3px 10px;border-radius:5px;'>3rd col — " + label3 + "</span></td>"
                : "";

        // Config table
        String configTable = buildConfigTable(
                safe(req.getThreadPoolSize(), "—"),
                safe(req.getJobTime(), "—"),
                safe(req.getSortable(), "—")
        );

        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;padding:0;background:#f0efec;font-family:'Helvetica Neue',Arial,sans-serif;">
        <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f0efec;padding:32px 0;">
          <tr><td align="center">
            <table width="680" cellpadding="0" cellspacing="0"
                   style="background:#ffffff;border-radius:12px;border:1px solid #e0dfd8;overflow:hidden;">
              <tr>
                <td style="background:#1a1a18;padding:28px 32px;">
                  <p style="margin:0;font-size:11px;color:#888;letter-spacing:0.1em;text-transform:uppercase;">Performance Report</p>
                  <h1 style="margin:6px 0 0;font-size:22px;font-weight:500;color:#ffffff;">%s</h1>
                  <p style="margin:6px 0 0;font-size:12px;color:#888;">Generated on %s</p>
                </td>
              </tr>
              <tr>
                <td style="padding:20px 32px 0;">
                  <table cellpadding="0" cellspacing="0"><tr>
                    <td style="padding-right:12px;"><span style="display:inline-block;background:#E6F1FB;color:#0C447C;font-size:11px;font-weight:500;padding:3px 10px;border-radius:5px;">1st col — %s</span></td>
                    <td style="padding-right:12px;"><span style="display:inline-block;background:#EAF3DE;color:#27500A;font-size:11px;font-weight:500;padding:3px 10px;border-radius:5px;">2nd col — %s</span></td>
                    %s
                  </tr></table>
                </td>
              </tr>
              <!-- Config table -->
              <tr>
                <td style="padding:20px 32px 0;">
                  <p style="margin:0 0 10px;font-size:11px;font-weight:500;color:#6b6b68;text-transform:uppercase;letter-spacing:0.06em;">Configuration</p>
                  %s
                </td>
              </tr>

              <!-- Performance table -->
              <tr>
                <td style="padding:20px 32px 32px;">
                  <p style="margin:0 0 10px;font-size:11px;font-weight:500;color:#6b6b68;text-transform:uppercase;letter-spacing:0.06em;">Performance Results</p>
                  <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;font-size:12px;">
                    <thead>
                      <tr style="background:#f7f7f5;">
                        <th style="padding:9px 10px;text-align:left;border:1px solid #e0dfd8;color:#6b6b68;font-weight:500;white-space:nowrap;">No of Jobs</th>
                        <th style="padding:9px 10px;text-align:left;border:1px solid #e0dfd8;color:#6b6b68;font-weight:500;">Nodes</th>
                        <th style="padding:9px 10px;text-align:left;border:1px solid #e0dfd8;color:#6b6b68;font-weight:500;">%s</th>
                        <th style="padding:9px 10px;text-align:left;border:1px solid #e0dfd8;color:#6b6b68;font-weight:500;">%s</th>
                        <th style="padding:9px 10px;text-align:left;border:1px solid #e0dfd8;color:#6b6b68;font-weight:500;">%s</th>
                        <th style="padding:9px 10px;text-align:left;border:1px solid #e0dfd8;color:#6b6b68;font-weight:500;">Improvement (2nd−1st)</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                </td>
              </tr>
              <tr>
                <td style="padding:16px 32px;border-top:1px solid #e0dfd8;background:#f7f7f5;">
                  <p style="margin:0;font-size:11px;color:#a0a09c;">
                    Sent by Mozart Performance Tool &nbsp;·&nbsp; This is an automated email.
                  </p>
                </td>
              </tr>
            </table>
          </td></tr>
        </table>
        </body></html>
        """.formatted(subject, sentAt, label1, label2, col3Legend, configTable,
                safe(req.getDate3(), "Date of 3rd col"), safe(req.getDate2(), "Date of 2nd col"), safe(req.getDate1(), "Date of 1st col"),
                tableRows.toString());
    }

    private String td(String val, String extraStyle) {
        String display = renderCell(val);
        return "<td style='padding:8px 10px;border:1px solid #e0dfd8;" + extraStyle + "'>" + display + "</td>";
    }

    private String tdStyled(String val, String style) {
        return "<td style='padding:8px 10px;border:1px solid #e0dfd8;" + style + "'>" + renderCell(val) + "</td>";
    }

    private String renderCell(String val) {
        if (val == null || val.equals("—"))
            return "<span style='color:#c0bfb8;'>—</span>";
        if ("Not Run".equals(val))
            return "<span style='background:#f0efec;color:#a0a09c;font-size:10px;padding:2px 6px;border-radius:4px;'>Not Run</span>";
        return val;
    }

    private String getImprStyle(String val) {
        if (val == null || val.equals("—") || "Not Run".equals(val)) return "color:#a0a09c;";
        if (val.startsWith("-")) return "color:#A32D2D;font-weight:500;";
        if (val.startsWith("+")) return "color:#27500A;font-weight:500;";
        return "";
    }

    private String formatJobs(int jobs) {
        if (jobs >= 1000000) return (jobs / 1000000) + "M";
        if (jobs >= 1000) return String.format("%,d", jobs);
        return String.valueOf(jobs);
    }

    private String buildConfigTable(String threadPoolSize, String jobTime, String sortable) {
        String rowStyle = "border-bottom:1px solid #e0dfd8;";
        String keyStyle = "padding:9px 14px;font-size:12px;font-weight:500;color:#6b6b68;background:#f7f7f5;border:1px solid #e0dfd8;width:160px;white-space:nowrap;";
        String valStyle = "padding:9px 14px;font-size:12px;color:#1a1a18;border:1px solid #e0dfd8;";
        return "<table cellpadding='0' cellspacing='0' style='border-collapse:collapse;font-size:12px;min-width:340px;'>"
                + "<tr style='" + rowStyle + "'><td style='" + keyStyle + "'>Thread Pool Size</td><td style='" + valStyle + "'>" + threadPoolSize + "</td></tr>"
                + "<tr style='" + rowStyle + "'><td style='" + keyStyle + "'>Job Time</td><td style='" + valStyle + "'>" + jobTime + "</td></tr>"
                + "<tr><td style='" + keyStyle + "'>Sortable</td><td style='" + valStyle + "'>" + sortable + "</td></tr>"
                + "</table>";
    }

    private String safe(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}