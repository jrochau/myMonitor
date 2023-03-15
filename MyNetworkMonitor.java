package JavaHIDP;

import java.awt.*;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import javax.swing.SwingWorker;
import com.sun.management.OperatingSystemMXBean;

public class MyNetworkMonitor extends JFrame {
    private JLabel cpuLabel;
    private JLabel memLabel;

    public MyNetworkMonitor() {
        super("My Monitor");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 3));

        // CPU and memory labels
        cpuLabel = new JLabel("CPU usage: N/A");
        cpuLabel.setHorizontalAlignment(JLabel.CENTER);

        memLabel = new JLabel("Memory usage: N/A");
        memLabel.setHorizontalAlignment(JLabel.CENTER);

        mainPanel.add(cpuLabel);
        mainPanel.add(memLabel);

        // Network statistics
        JTextArea networkStatsArea = new JTextArea();
        networkStatsArea.setEditable(false);
        JScrollPane networkStatsScrollPane = new JScrollPane(networkStatsArea);

        mainPanel.add(networkStatsScrollPane);

        add(mainPanel);

        setSize(900, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        startMonitoring();
        updateNetworkStatistics(networkStatsArea);
    }

    private void startMonitoring() {
        SwingWorker<Void, Double[]> worker = new SwingWorker<>() {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

            @Override
            protected Void doInBackground() {
                while (true) {
                    double cpuUsage = osBean.getSystemCpuLoad() * 100;
                    double memUsage = getMemoryUsage();
                    publish(new Double[]{cpuUsage, memUsage});
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void process(java.util.List<Double[]> chunks) {
                Double[] values = chunks.get(chunks.size() - 1);
                double cpuUsage = values[0];
                double memUsage = values[1];
                cpuLabel.setText("CPU usage: " + String.format("%.2f", cpuUsage) + "%");
                memLabel.setText("Memory usage: " + String.format("%.2f", memUsage) + "%");
            }
        };
        worker.execute();
    }

    private static double getMemoryUsage() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double usedMemory = osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize();
        double maxMemory = osBean.getTotalPhysicalMemorySize();
        return usedMemory / maxMemory * 100.0;
    }

    private void updateNetworkStatistics(JTextArea textArea) {
        Timer timer = new Timer(1000, e -> {
            try {
                String networkStats = getNetworkStats();
                textArea.setText(networkStats);
            } catch (IOException ex) {
                textArea.append("Error: " + ex.getMessage() + "\n");
            }
        });

        timer.start();
    }

    private static String getNetworkStats() throws IOException {
    StringBuilder output = new StringBuilder();
    String command = "netstat -s";

    Process process = Runtime.getRuntime().exec(command);
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

    String line;
    boolean readingIPv4Stats = false;
    while ((line = reader.readLine()) != null) {
        if (line.trim().startsWith("IPv4")) {
            readingIPv4Stats = true;
            output.append(line).append("\n");
        } else if (line.trim().startsWith("IPv6")) {
            readingIPv4Stats = false;
        } else if (readingIPv4Stats) {
            output.append(line).append("\n");
        }
    }

    return output.toString();
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MyNetworkMonitor();
        });
    }
}
