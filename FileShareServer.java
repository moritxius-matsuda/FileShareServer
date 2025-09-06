import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import javax.swing.*;

public class FileShareServer {
    private static HttpServer server;
    private static String sharedFilePath;
    private static String fileName;
    
    public static void main(String[] args) {
        // Wenn keine Argumente übergeben wurden, öffne Datei-Dialog
        if (args.length == 0) {
            sharedFilePath = selectFile();
            if (sharedFilePath == null) {
                return; // Benutzer hat abgebrochen
            }
        } else {
            sharedFilePath = args[0];
        }
        
        File file = new File(sharedFilePath);
        
        if (!file.exists()) {
            showError("Datei existiert nicht: " + sharedFilePath);
            return;
        }
        
        fileName = file.getName();
        
        try {
            startServer();
        } catch (Exception e) {
            showError("Fehler beim Starten des Servers: " + e.getMessage());
        }
    }
    
    private static String selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Datei zum Teilen auswählen");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        // Home-Verzeichnis als Startpunkt
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        
        int result = fileChooser.showOpenDialog(null);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        
        return null; // Benutzer hat abgebrochen
    }
    
    private static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8181), 0);
        
        server.createContext("/download", new FileHandler());
        server.createContext("/", new InfoHandler());
        
        server.setExecutor(null);
        server.start();
        
        String url = "http://localhost:8181";
        String downloadUrl = url + "/download";
        
        Toolkit.getDefaultToolkit().getSystemClipboard()
               .setContents(new StringSelection(downloadUrl), null);
        
        showNotification(downloadUrl);
        
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            System.err.println("Konnte Browser nicht öffnen: " + e.getMessage());
        }
    }
    
    static class FileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File(sharedFilePath);
            
            if (!file.exists()) {
                String response = "Datei nicht gefunden!";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            
            String contentType = getContentType(fileName);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Content-Disposition", 
                "attachment; filename=\"" + fileName + "\"");
            
            exchange.sendResponseHeaders(200, file.length());
            
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = exchange.getResponseBody();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            fis.close();
            os.close();
        }
    }
    
    static class InfoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String response = createInfoPage();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }
    }
    
    private static String createInfoPage() {
        File file = new File(sharedFilePath);
        long fileSize = file.length();
        String fileSizeStr = formatFileSize(fileSize);
        
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'>" +
               "<title>File Sharing Server</title>" +
               "<style>body{font-family:Arial,sans-serif;margin:40px;background:#f5f5f5}" +
               ".container{background:white;padding:30px;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1)}" +
               ".download-btn{background:#007bff;color:white;padding:12px 24px;text-decoration:none;border-radius:4px;display:inline-block;margin:10px 0}" +
               ".download-btn:hover{background:#0056b3}" +
               ".info{background:#e9ecef;padding:15px;border-radius:4px;margin:15px 0}" +
               "</style></head><body>" +
               "<div class='container'>" +
               "<h1>File Sharing Server</h1>" +
               "<div class='info'>" +
               "<strong>Datei:</strong> " + fileName + "<br>" +
               "<strong>Größe:</strong> " + fileSizeStr + "<br>" +
               "<strong>Server:</strong> http://localhost:8181" +
               "</div>" +
               "<a href='/download' class='download-btn'>Datei herunterladen</a>" +
               "<p><small>Die Download-URL wurde in die Zwischenablage kopiert!</small></p>" +
               "<p><small>Server läuft auf Port 8181. Zum Beenden dieses Fenster schließen.</small></p>" +
               "</div></body></html>";
    }
    
    private static String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (extension.equals("pdf")) return "application/pdf";
        if (extension.equals("txt")) return "text/plain";
        if (extension.equals("jpg") || extension.equals("jpeg")) return "image/jpeg";
        if (extension.equals("png")) return "image/png";
        if (extension.equals("gif")) return "image/gif";
        if (extension.equals("zip")) return "application/zip";
        if (extension.equals("doc")) return "application/msword";
        if (extension.equals("docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (extension.equals("xls")) return "application/vnd.ms-excel";
        if (extension.equals("xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }
    
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private static void showNotification(String url) {
        JFrame frame = new JFrame("File Sharing Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 250);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Server gestartet!");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel fileLabel = new JLabel("Datei: " + fileName);
        fileLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel urlLabel = new JLabel("URL: " + url);
        urlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel clipboardLabel = new JLabel("Download-URL in Zwischenablage kopiert!");
        clipboardLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        clipboardLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        
        JButton stopButton = new JButton("Server stoppen");
        stopButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (server != null) {
                    server.stop(0);
                }
                System.exit(0);
            }
        });
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(fileLabel);
        panel.add(urlLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(clipboardLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(stopButton);
        
        frame.add(panel);
        frame.setVisible(true);
    }
    
    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }
}