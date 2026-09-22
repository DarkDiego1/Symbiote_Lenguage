package symbiote;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Symbiote {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(SymbioteGUI::new);
    }
}