package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ArchetypeManagerDialog extends JDialog {

    public ArchetypeManagerDialog(JFrame owner) {
        super(owner, "Manage archetypes", true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(760, 460));

        add(new JPanel(), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }
}
