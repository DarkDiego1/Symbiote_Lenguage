package symbiote;

import java.awt.*;
import javax.swing.*;

public class SplashScreen extends JWindow {

    public SplashScreen() {
        ImageIcon icono = new ImageIcon(getClass().getResource("symbiote_logo.png"));
        JLabel imagen = new JLabel(icono);
        imagen.setBorder(BorderFactory.createEmptyBorder(36, 36, 10, 36));

        JLabel texto = new JLabel("Cargando Symbiote...", SwingConstants.CENTER);
        texto.setFont(new Font("SansSerif", Font.PLAIN, 13));
        texto.setForeground(new Color(100, 100, 105));
        texto.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);
        barra.setForeground(new Color(134, 97, 255));
        barra.setBorder(BorderFactory.createEmptyBorder(0, 60, 28, 60));

        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(Color.WHITE);
        pie.add(texto, BorderLayout.NORTH);
        pie.add(barra, BorderLayout.CENTER);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Color.WHITE);
        raiz.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 230), 1));
        raiz.add(imagen, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);

        setContentPane(raiz);
        pack();
        setLocationRelativeTo(null);
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