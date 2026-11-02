package com.goldenchemist.pos.view;

import com.goldenchemist.pos.controller.AuthController;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.Component;
import java.awt.GridLayout;

public final class ChangePasswordDialog {

    private ChangePasswordDialog() {
    }

    public static void show(Component parent) {
        JPasswordField oldPw = new JPasswordField(18);
        JPasswordField newPw = new JPasswordField(18);
        JPasswordField confirm = new JPasswordField(18);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 4));
        p.add(new JLabel("Current password"));
        p.add(oldPw);
        p.add(new JLabel("New password (min. 6 characters)"));
        p.add(newPw);
        p.add(new JLabel("Repeat new password"));
        p.add(confirm);
        if (JOptionPane.showConfirmDialog(parent, p, "Change password", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        if (Ui.guard(parent, () -> new AuthController().changeOwnPassword(new String(oldPw.getPassword()),
                new String(newPw.getPassword()), new String(confirm.getPassword())))) {
            Ui.info(parent, "Your password has been changed.");
        }
    }
}
