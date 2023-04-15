import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Optional;
import java.lang.management.ThreadInfo;

import com.sun.management.OperatingSystemMXBean;

public class MyMonitor extends JFrame {
    private JLabel cpuLabel;
    private JLabel memLabel;

    public MyMonitor() {
        super("My Monitor");
    
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    
        // CPU and memory labels
        JPanel resourcePanel = new JPanel();
        resourcePanel.setLayout(new GridLayout(2, 1));
        cpuLabel = new JLabel("CPU usage: N/A");
        cpuLabel.setHorizontalAlignment(JLabel.CENTER);
    
        memLabel = new JLabel("Memory usage: N/A");
        memLabel.setHorizontalAlignment(JLabel.CENTER);
    
        resourcePanel.add(cpuLabel);
        resourcePanel.add(memLabel);
        mainPanel.add(resourcePanel);
    
        // Process monitoring
        DefaultTableModel processesTableModel = new DefaultTableModel(new String[]{"PID", "Name", "CPU Usage", "Memory Usage"}, 0);
        JTable processesTable = new JTable(processesTableModel);
        JScrollPane processesScrollPane = new JScrollPane(processesTable);
        processesScrollPane.setPreferredSize(new Dimension(300, 100));
        mainPanel.add(processesScrollPane);
    
        // Network statistics
        JTextArea networkStatsArea = new JTextArea();
        networkStatsArea.setEditable(false);
        networkStatsArea.setRows(10);
        networkStatsArea.setColumns(30);
        networkStatsArea.setLineWrap(true);
        networkStatsArea.setWrapStyleWord(true);
    
        mainPanel.add(networkStatsArea);

        add(mainPanel);
    
        // Set window size based on inches
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        int width = (int) (3.5 * dpi);
        int height = (int) (5 * dpi);
        setSize(width, height);
    
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    
        startMonitoring();
        updateNetworkStatistics(networkStatsArea);
        updateProcesses(processesTableModel);
    }
    private void updateProcesses(DefaultTableModel processesTableModel) {
        Timer timer = new Timer(1000, e -> {
            processesTableModel.setRowCount(0); // Clear the table
            ProcessHandle.allProcesses().forEach(processHandle -> {
                ProcessHandle.Info processInfo = processHandle.info();
                if (processInfo.command().isPresent()) {
                    String[] rowData = new String[4];
                    rowData[0] = Long.toString(processHandle.pid());
                    rowData[1] = processInfo.command().orElse("Unknown");
                    rowData[2] = getProcessCpuUsage(processHandle).map(val -> String.format("%.2f", val * 100) + "%").orElse("N/A");
                    rowData[3] = processInfo.totalCpuDuration().isPresent() ? processInfo.totalCpuDuration().get().toString() : "N/A";
                    processesTableModel.addRow(rowData);
                }
            });
        });
    
        timer.start();
    }

    private Optional<Double> getProcessCpuUsage(ProcessHandle processHandle) {
        Optional<Double> cpuUsage = Optional.empty();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] ids = threadMXBean.getAllThreadIds();
        for (long id : ids) {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(id, Integer.MAX_VALUE);
            if (threadInfo != null && threadInfo.getThreadId() == processHandle.pid()) {
                long cpuTime = threadMXBean.getThreadCpuTime(id);
                long upTime = ManagementFactory.getRuntimeMXBean().getUptime() * 1000000;
                double cpuUsagePercentage = ((double) cpuTime / upTime) * 100.0;
                cpuUsage = Optional.of(cpuUsagePercentage);
                break;
            }
        }
        return cpuUsage;
    }

    private void startMonitoring() {
        SwingWorker<Void, Double[]> worker = new SwingWorker<>() {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            private final double MEMORY_THRESHOLD = 80; // Set the memory threshold to 80%
            private boolean alertShown = false; // Flag to indicate if an alert has already been shown

        @Override
        protected Void doInBackground() {
            while (!isCancelled()) {
                double cpuUsage = osBean.getSystemCpuLoad() * 100;
                double memUsage = getMemoryUsage();
                publish(new Double[]{cpuUsage, memUsage});
                if (memUsage >= MEMORY_THRESHOLD && !alertShown) { // Trigger alert if memory usage exceeds threshold
                    JOptionPane.showMessageDialog(MyMonitor.this, "High memory usage detected!", "Alert", JOptionPane.WARNING_MESSAGE);
                    alertShown = true;
                } else if (memUsage < MEMORY_THRESHOLD) {
                    alertShown = false;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            return null;
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

    private void updateNetworkStatistics(JTextArea networkStatsArea) {
        Timer timer = new Timer(1000, e -> {
            try {
                String networkStats = getNetworkStats();
                networkStatsArea.setText(networkStats);
            } catch (IOException ex) {
                networkStatsArea.append("Error: " + ex.getMessage() + "\n");
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
                if (!line.contains("Reassembly Required") && !line.contains("Reassembly Successful") && !line.contains("Reassembly Failures") && !line.contains("Fragments Created") && !line.contains("Datagrams Failing Fragmentation")) {
                    output.append(line).append("\n");
                }
            }
        }

        return output.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MyMonitor();
        });
    }
}
