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

        boolean running = true;
        while (running) {
            System.out.println("\n--- Cyberbullying Evidence Collector ---");
            System.out.println("1. Take Screenshot");
            System.out.println("2. Generate HTML Report");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    captureScreenshot(caseId);
                    break;
                case "2":
                    generateHtmlReport(caseId);
                    break;
                case "3":
                    running = false;
                    System.out.println("Exiting program...");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    public static void captureScreenshot(String caseId) {
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

            // Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = new FileInputStream(screenshotFile)) {
                byte[] byteArray = fis.readAllBytes();
                byte[] hashBytes = digest.digest(byteArray);

                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                String hash = sb.toString();

                // Append metadata to CSV
                File logFile = new File(caseFolder.toFile(), "evidence_log.csv");
                boolean newFile = logFile.createNewFile();
                try (FileWriter writer = new FileWriter(logFile, true)) {
                    if (newFile) {
                        writer.write("Screenshot Filename,Timestamp,SHA-256 Hash\n");
                    }
                    writer.write(screenshotFileName + "," + readableTime + "," + hash + "\n");
                }

                System.out.println("Metadata logged to evidence_log.csv");
            }

        } catch (AWTException e) {
            System.err.println("Error initializing screen capture: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error during screenshot save or log: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not available: " + e.getMessage());
        }
    }

    public static String computeSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
    public static void generateHtmlReport(String caseId) {
        try {
            String caseFolder = "Cases/" + caseId;
            File folder = new File(caseFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File logFile = new File(caseFolder + "/evidence_log.csv");
            String screenshotFileName = null;

            if (logFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                    reader.readLine(); // Skip header
                    String lastLine = null;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lastLine = line;
                    }
                    if (lastLine != null) {
                        String[] parts = lastLine.split(",", 3);
                        if (parts.length == 3) {
                            screenshotFileName = parts[0].trim();
                        }
                    }
                }
            }
            FileWriter writer = new FileWriter(caseFolder + "/report.html");
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
            writer.write("<p><strong>Timestamp:</strong> " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>");
            writer.write("<h2>Screenshot</h2>");
            if (screenshotFileName != null) {
                writer.write("<img src='" + screenshotFileName + "' alt='Screenshot' " +
                        "style='max-width:100%; border:1px solid #ccc;'/>");
            }
            else {
                writer.write("<p>No screenshot found.</p>");
            }
            writer.write("<h2>Metadata</h2>");
            writer.write("<table>");
            writer.write("<tr><th>Key</th><th>Value</th></tr>");
            if (logFile.exists()) {
                try (Scanner reader = new Scanner(logFile)) {
                    reader.nextLine(); // Skip header
                    while (reader.hasNextLine()) {
                        String line = reader.nextLine();
                        String[] parts = line.split(",", 3);
                        if (parts.length == 3) {
                            writer.write("<tr><td>Screenshot Filename</td><td>" + parts[0].trim() + "</td></tr>");
                            writer.write("<tr><td>Timestamp</td><td>" + parts[1].trim() + "</td></tr>");
                            writer.write("<tr><td>SHA-256 Hash</td><td><code>" + parts[2].trim() + "</code></td></tr>");
                        }
                    }
                }
            }
            else {
                writer.write("<tr><td colspan='2'>No metadata found.</td></tr>");
            }
            writer.write("</table>");
            writer.write("</body></html>");
            writer.close();
            System.out.println("HTML report generated: " + caseFolder + "/report.html");
        }
        catch (IOException e) {
            System.out.println("Error generating HTML report: " + e.getMessage());
        }
    }
}