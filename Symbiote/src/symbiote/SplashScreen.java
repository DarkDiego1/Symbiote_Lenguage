package symbiote;

import java.awt.*;
import javax.swing.*;

public class SplashScreen extends JWindow {

    public SplashScreen() {
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("symbiote_logo.png"));
        int targetWidth = 680; 
        int originalWidth = originalIcon.getIconWidth();
        int originalHeight = originalIcon.getIconHeight();
        int targetHeight = (originalWidth > 0) ? (targetWidth * originalHeight) / originalWidth : 340;
        
        Image scaledImage = originalIcon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        ImageIcon icono = new ImageIcon(scaledImage);

        JLabel imagen = new JLabel(icono);
        imagen.setBorder(BorderFactory.createEmptyBorder(45, 45, 45, 45));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Color.WHITE);
        raiz.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 230), 1));
        raiz.add(imagen, BorderLayout.CENTER);

        setContentPane(raiz);
        pack();
        setLocationRelativeTo(null);
    }

    public void mostrar(Runnable alTerminar) {
        mostrar(5000, alTerminar);
    }

    public void mostrar(int milisegundos, Runnable alTerminar) {
        setVisible(true);
        Timer t = new Timer(milisegundos, e -> {
            setVisible(false);
            dispose();
            alTerminar.run();
        });
        t.setRepeats(false);
        t.start();
    }
}