package net.tawkit.remotebox.ui;

import net.tawkit.remotebox.model.BoxProfile;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class ProfileDialog extends JDialog {

    private boolean saved = false;
    private final BoxProfile result;

    ProfileDialog(Window owner, String host, BoxProfile in) {
        super(owner, "Profil scrcpy — " + host, ModalityType.APPLICATION_MODAL);
        this.result = new BoxProfile();

        JTextField label = new JTextField(in.label, 28);
        JSpinner port = new JSpinner(new SpinnerNumberModel(in.adbPort, 1, 65535, 1));
        JCheckBox autoConnect = new JCheckBox("Faire `adb connect` avant scrcpy", in.autoAdbConnect);
        JTextArea args = new JTextArea(String.join(" ", in.scrcpyArgs), 4, 40);
        args.setLineWrap(true);
        args.setWrapStyleWord(true);
        JTextArea notes = new JTextArea(in.notes, 3, 40);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);

        JButton presetHw = new JButton("Préréglage : encodeur matériel");
        JButton presetSw = new JButton("Préréglage : encodeur logiciel");
        presetHw.addActionListener(e -> args.setText(String.join(" ", BoxProfile.defaultHardware().scrcpyArgs)));
        presetSw.addActionListener(e -> args.setText(String.join(" ", BoxProfile.defaultSoftware().scrcpyArgs)));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;

        g.gridx = 0; g.gridy = y; form.add(new JLabel("Libellé :"), g);
        g.gridx = 1; g.weightx = 1; form.add(label, g); y++;
        g.gridx = 0; g.gridy = y; g.weightx = 0; form.add(new JLabel("Port adb :"), g);
        g.gridx = 1; form.add(port, g); y++;
        g.gridx = 1; g.gridy = y; form.add(autoConnect, g); y++;
        g.gridx = 0; g.gridy = y; form.add(new JLabel("Arguments scrcpy :"), g);
        g.gridx = 1; g.weightx = 1; form.add(new JScrollPane(args), g); y++;
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        presets.add(presetHw);
        presets.add(presetSw);
        g.gridx = 1; g.gridy = y; form.add(presets, g); y++;
        g.gridx = 1; g.gridy = y;
        form.add(new JLabel("<html><span style='font-size:9px;color:gray'>"
                + "-s ip:port et --window-title sont ajoutés automatiquement.</span></html>"), g);
        y++;
        g.gridx = 0; g.gridy = y; g.weightx = 0; form.add(new JLabel("Notes :"), g);
        g.gridx = 1; g.weightx = 1; form.add(new JScrollPane(notes), g); y++;

        JButton ok = new JButton("Enregistrer");
        JButton cancel = new JButton("Annuler");
        ok.addActionListener(e -> {
            result.label = label.getText().trim();
            result.adbPort = (Integer) port.getValue();
            result.autoAdbConnect = autoConnect.isSelected();
            result.scrcpyArgs = tokenize(args.getText());
            result.notes = notes.getText().trim();
            saved = true;
            dispose();
        });
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        for (String part : s.trim().split("\\s+")) {
            if (!part.isBlank()) out.add(part);
        }
        return out;
    }

    boolean isSaved() {
        return saved;
    }

    BoxProfile getResult() {
        return result;
    }
}
