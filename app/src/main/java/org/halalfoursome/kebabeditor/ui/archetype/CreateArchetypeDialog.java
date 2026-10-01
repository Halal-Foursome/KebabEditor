package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class CreateArchetypeDialog extends JDialog {

    private final JTextField idField = new JTextField();
    private final JTextField displayNameField = new JTextField();
    private final JTextField iconPathField = new JTextField();
    private final JPanel paramsPanel = new JPanel();
    private File iconFile;

    public CreateArchetypeDialog(JDialog owner, Consumer<ArchetypeDefinition> consumer) {
        super(owner, "Create new archetype", true);

        KebabStyle style = KebabStyle.getCurrent();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(540, 420));

        add(buildCenter(style), BorderLayout.CENTER);
        add(buildFooter(style), BorderLayout.SOUTH);

        setLocationRelativeTo(owner);
    }

    private JComponent buildCenter(KebabStyle style) {
        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 16, 4, 16);

        // id
        center.add(label("ID", style), constraints);
        idField.setFont(style.uiFont());
        center.add(idField, constraints);
        JLabel idNote = label("The ID cannot be changed after the archetype is created.", style);
        idNote.setFont(style.uiFont().deriveFont(Font.ITALIC, style.uiFont().getSize2D() - 1f));
        center.add(idNote, constraints);

        // display name
        center.add(label("Display name", style), constraints);
        displayNameField.setFont(style.uiFont());
        center.add(displayNameField, constraints);

        // icon
        center.add(label("Icon", style), constraints);
        center.add(buildIconPicker(style), constraints);

        // params
        JPanel paramsHeader = new JPanel(new BorderLayout());
        paramsHeader.add(label("Parameters", style), BorderLayout.WEST);
        JButton addButton = new JButton("Add");
        addButton.setFont(style.uiFont());
        addButton.addActionListener(e -> addParamRow(style));
        paramsHeader.add(addButton, BorderLayout.EAST);
        center.add(paramsHeader, constraints);

        paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.Y_AXIS));
        JPanel paramsWrapper = new JPanel(new BorderLayout());
        paramsWrapper.add(paramsPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(paramsWrapper);
        scrollPane.setPreferredSize(new Dimension(0, 140));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(4, 16, 8, 16);
        center.add(scrollPane, constraints);

        return center;
    }

    private JLabel label(String text, KebabStyle style) {
        JLabel label = new JLabel(text);
        label.setFont(style.uiFont());
        return label;
    }

    private JComponent buildIconPicker(KebabStyle style) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));

        iconPathField.setEditable(false);
        iconPathField.setFont(style.uiFont());
        panel.add(iconPathField, BorderLayout.CENTER);

        JButton browse = new JButton("Browse...");
        browse.setFont(style.uiFont());
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "Images (png, jpg, gif, svg)", "png", "jpg", "jpeg", "gif", "svg"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                iconFile = chooser.getSelectedFile();
                iconPathField.setText(iconFile.getAbsolutePath());
            }
        });
        panel.add(browse, BorderLayout.EAST);

        return panel;
    }

    private void addParamRow(KebabStyle style) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField();
        nameField.setFont(style.uiFont());
        c.weightx = 1.0;
        row.add(nameField, c);

        JComboBox<ParamType> typeBox = new JComboBox<>(ParamType.values());
        typeBox.setFont(style.uiFont());
        typeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list,
                        value instanceof ParamType type ? type.displayName() : value,
                        index, isSelected, cellHasFocus);
            }
        });
        c.weightx = 0;
        row.add(typeBox, c);

        JButton removeButton = new JButton("Remove");
        removeButton.setFont(style.uiFont());
        removeButton.addActionListener(e -> {
            paramsPanel.remove(row);
            paramsPanel.revalidate();
            paramsPanel.repaint();
        });
        row.add(removeButton, c);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        paramsPanel.add(row);
        paramsPanel.revalidate();
        paramsPanel.repaint();
    }

    private JPanel buildFooter(KebabStyle style) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JButton createButton = new JButton("Create");
        createButton.setFont(style.uiFont());
        createButton.addActionListener(e -> {
            
        });
        footer.add(createButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(style.uiFont());
        cancelButton.addActionListener(e -> dispose());
        footer.add(cancelButton);

        return footer;
    }
}
