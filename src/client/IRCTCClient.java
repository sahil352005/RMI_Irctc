package client;

import gui.IRCTCMainFrame;
import javax.swing.SwingUtilities;

public class IRCTCClient {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IRCTCMainFrame().setVisible(true);
        });
    }
} 