package JavaHIDP;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class MyNetworkMonitor extends JFrame {
    private final JLabel cpuLabel;
    private final JLabel memLabel;

    public MyNetworkMonitor() {
        super("My Monitor");

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 1));

        cpuLabel = new JLabel("CPU usage: N/A");
        cpuLabel.setHorizontalAlignment(JLabel.CENTER);

        memLabel = new JLabel("Memory usage: N/A");
        memLabel.setHorizontalAlignment(JLabel.CENTER);

        mainPanel.add(cpuLabel);
        mainPanel.add(memLabel);

        add(mainPanel);

        setSize(300, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        startMonitoring();
    }

    public static void main(String[] args) {
        new MyNetworkMonitor();
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
}
