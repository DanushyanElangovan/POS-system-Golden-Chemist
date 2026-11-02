package com.goldenchemist.pos;

import com.goldenchemist.pos.controller.AuthController;
import com.goldenchemist.pos.db.Database;
import com.goldenchemist.pos.util.PosException;
import com.goldenchemist.pos.view.LoginView;
import com.goldenchemist.pos.view.Theme;
import com.goldenchemist.pos.view.Ui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Golden Chemist Pharmacy POS - application entry point. */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        while (true) {
            try {
                Database.verifyConnection();
                boolean firstRun = new AuthController().ensureDefaultAdmin();
                new LoginView(firstRun).setVisible(true);
                return;
            } catch (PosException e) {
                Object[] options = {"Retry", "Exit"};
                int choice = JOptionPane.showOptionDialog(null, e.getMessage(), "Golden Chemist POS - database problem",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
                if (choice != 0) {
                    System.exit(1);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                Ui.error(null, "The application could not start: " + e);
                System.exit(1);
            }
        }
    }
}
