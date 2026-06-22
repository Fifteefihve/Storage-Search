import javax.swing.*;

public static void main(String[] args) {
    String path = args.length > 0 ? args[0] : "src/components.xlsx";
    SwingUtilities.invokeLater(() -> {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception e) {}
        new MainWindow(path).setVisible(true);
    });
}