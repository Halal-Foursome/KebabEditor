package org.halalfoursome.kebabeditor.ui.project;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.halalfoursome.kebabeditor.utils.FileChooser;
import org.halalfoursome.kebabeditor.utils.FileChooser.FileChooserMode;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class CreateProjectDialog extends JDialog {

    private final JTextField locationField = new JTextField(24);
    private final JTextField nameField = new JTextField("Untitled", 24);


    private final JComboBox<String> templateField = new JComboBox<>(new String[] {
        "blank",
        "default preset",
    });

    private final JCheckBox openAfterCreateCheckBox = new JCheckBox("Open after create", true);
    private final JCheckBox createFolderCheckBox = new JCheckBox("Create folder for project", true);

    JButton createButton = new JButton("Create");
    JButton cancelButton = new JButton("Cancel");

    public CreateProjectDialog(JFrame owner) {
        super(owner, "New Project", true);

        KebabStyle style = KebabStyle.getCurrent();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(760, 460));

        add(buildHeader(style), BorderLayout.NORTH);
        add(buildSidebar(style), BorderLayout.WEST);
        add(buildMainPanel(style, owner), BorderLayout.CENTER);
        add(buildFooter(style), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildHeader(KebabStyle style) {
        JPanel header = new JPanel();
        header.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 12, 10, 12);

        JLabel title = new JLabel("Create new project");
        title.setFont(style.uiFont().deriveFont(24f));
        constraints.gridy = 0;
        header.add(title, constraints);

        constraints.gridy = 1;
        header.add(new JSeparator(SwingConstants.HORIZONTAL), constraints);

        return header;
    }

    private JPanel buildSidebar(KebabStyle style) {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 12, 10, 12);

        JLabel title = new JLabel("Options");
        title.setFont(style.uiFont().deriveFont(18f));
        constraints.gridy = 0;
        sidebar.add(title, constraints);

        constraints.gridy = 1;
        sidebar.add(new JSeparator(SwingConstants.HORIZONTAL), constraints);

        constraints.gridy = 2;
        sidebar.add(labeledRow(style, "Template", templateField), constraints);

        constraints.gridy = 3;
        sidebar.add(openAfterCreateCheckBox, constraints);

        constraints.gridy = 4;
        createFolderCheckBox.addActionListener(e -> {
            nameField.setEnabled(createFolderCheckBox.isSelected());
        });
        sidebar.add(createFolderCheckBox, constraints);

        constraints.gridy = 5;
        constraints.weighty = 1.0;
        sidebar.add(new JPanel(), constraints);

        return sidebar;
    }

    private JPanel buildMainPanel(KebabStyle style, JFrame owner) {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(54, 16, 8, 16);

        constraints.gridy = 0;
        content.add(new JSeparator(SwingConstants.HORIZONTAL), constraints);

        constraints.gridy = 1;
        content.add(labeledRow(style, "Location", buildLocationRow(owner)), constraints);

        constraints.gridy = 2;
        content.add(labeledRow(style, "Folder name", nameField), constraints);

        constraints.gridy = 3;
        constraints.weighty = 1.0;
        content.add(new JPanel(), constraints);

        return content;
    }

    private JPanel buildLocationRow(JFrame owner) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        locationField.getDocument().addDocumentListener(new DocumentListener() {
        @Override 
        public void changedUpdate(DocumentEvent e) {
            createButton.setEnabled(!locationField.getText().trim().isEmpty());
        }
        @Override 
        public void removeUpdate(DocumentEvent e) {
            createButton.setEnabled(!locationField.getText().trim().isEmpty());
        }
        @Override 
        public void insertUpdate(DocumentEvent e) {
            createButton.setEnabled(!locationField.getText().trim().isEmpty());
        }
        });
        row.add(locationField, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> {
            FileChooser chooser = new FileChooser(FileChooserMode.DIRS);
            chooser.open(owner, selected -> locationField.setText(selected.getAbsolutePath()));
        });
        row.add(browseButton, BorderLayout.EAST);

        return row;
    }

    private JPanel buildFooter(KebabStyle style) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));

        cancelButton.addActionListener(e -> dispose());

        createButton.addActionListener(e -> dispose());
        createButton.setEnabled(false);

        footer.add(cancelButton);
        footer.add(createButton);

        return footer;
    }

    private JPanel labeledRow(KebabStyle style, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 6));

        JLabel text = new JLabel(label);
        text.setFont(style.uiFont());

        row.add(text, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);

        return row;
    }
}
