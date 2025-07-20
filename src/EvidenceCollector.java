// src/EvidenceCollector.java
package com.cybersafety.evidence;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class EvidenceCollector {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter a case ID (e.g., Case001): ");
        String caseId = scanner.nextLine().trim();

        if (caseId.isEmpty()) {
            System.out.println("Case ID cannot be empty. Exiting.");
            return;
        }

        if (captureScreenshot(caseId)) {
            generateHtmlReport(caseId);
            System.out.println("Evidence and report generated for case: " + caseId);
        }
    }

    public static boolean captureScreenshot(String caseId) {
        try {
            Path caseFolder = Paths.get("Cases", caseId);
            Files.createDirectories(caseFolder);

            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String readableTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String screenshotFileName = "screenshot_" + timestamp + ".png";

            File screenshotFile = new File(caseFolder.toFile(), screenshotFileName);
            ImageIO.write(screenFullImage, "png", screenshotFile);
            System.out.println("Screenshot saved: " + screenshotFile.getAbsolutePath());

            String hash = computeSHA256(Files.readAllBytes(screenshotFile.toPath()));

            File logFile = new File(caseFolder.toFile(), "evidence_log.csv");
            boolean newFile = logFile.createNewFile();
            try (FileWriter writer = new FileWriter(logFile, true)) {
                if (newFile) {
                    writer.write("Screenshot Filename,Timestamp,SHA-256 Hash\n");
                }
                writer.write(screenshotFileName + "," + readableTime + "," + hash + "\n");
            }

            System.out.println("Metadata logged to evidence_log.csv");
            return true;

        } catch (AWTException e) {
            System.err.println("Error initializing screen capture: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error during screenshot save or log: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not available: " + e.getMessage());
        }
        return false;
    }

    public static String computeSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void generateHtmlReport(String caseId) {
        String caseFolder = "Cases/" + caseId;
        File logFile = new File(caseFolder + "/evidence_log.csv");
        File htmlReport = new File(caseFolder + "/report.html");

        try (FileWriter writer = new FileWriter(htmlReport)) {
            writer.write("<!DOCTYPE html>");
            writer.write("<html><head><title>Cyberbullying Case Report</title>");
            writer.write("<style>");
            writer.write("body { font-family: Arial, sans-serif; padding: 20px; }");
            writer.write("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
            writer.write("th, td { border: 1px solid #ddd; padding: 8px; }");
            writer.write("th { background-color: #f2f2f2; }");
            writer.write("</style>");
            writer.write("</head><body>");
            writer.write("<h1>Cyberbullying Case Report: " + caseId + "</h1>");
            writer.write("<p><strong>Timestamp:</strong> " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>");
            writer.write("<h2>Evidence</h2>");
            writer.write("<table>");
            writer.write("<tr><th>Screenshot</th><th>Timestamp</th><th>SHA-256 Hash</th></tr>");

            if (logFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                    reader.readLine(); // skip header
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",", 3);
                        if (parts.length == 3) {
                            writer.write("<tr>");
                            writer.write("<td><a href=\"" + parts[0] + "\">" + parts[0] + "</a></td>");
                            writer.write("<td>" + parts[1] + "</td>");
                            writer.write("<td><code>" + parts[2] + "</code></td>");
                            writer.write("</tr>");
                        }
                    }
                }
            } else {
                writer.write("<tr><td colspan='3'>No evidence found.</td></tr>");
            }

            writer.write("</table>");
            writer.write("</body></html>");
            System.out.println("HTML report generated: " + htmlReport.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error generating HTML report: " + e.getMessage());
        }
    }
}