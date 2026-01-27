import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import com.google.gson.JsonParser;

public static void main() {
    JFrame frame1 = new JFrame();
    frame1.setResizable(false);

    JButton button1 = new JButton("Start");
    button1.setSize(300, 30);
    button1.setBounds(300, 300,  100, 30);

    frame1.add(button1);
    frame1.setSize(720, 480);
    frame1.setLayout(null);
    frame1.setVisible(true);

    JLabel WelcomeMessage = new JLabel("Welcome to the Welcome Message");
    WelcomeMessage.setFont(new Font("Times New Roman", Font.BOLD, 24));
    WelcomeMessage.setVisible(true);
    WelcomeMessage.setForeground(Color.black);
    WelcomeMessage.setBounds(720, 400, 200, 100);
}