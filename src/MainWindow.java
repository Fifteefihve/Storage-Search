import javax.swing.*;

public class MainWindow {
    private JFrame frame1;
    private JPanel panel1;
    private JTree ComponentsList;

    public void MainSearch() {

        JFrame frame1 = new JFrame();
        frame1.setResizable(false);
        frame1.setSize(1920, 1080);
        //frame1.setLayout(null);
        frame1.setVisible(true);
        frame1.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame1.add(ComponentsList);
    }
}
