package org.halalfoursome.kebabeditor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.ui.menu.CreateProjectDialog;
import org.halalfoursome.kebabeditor.ui.menu.MenuBar;
import org.halalfoursome.kebabeditor.ui.menu.MenuButton;
import org.halalfoursome.kebabeditor.ui.menu.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.OptionsDialog;
import org.halalfoursome.kebabeditor.ui.menu.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.SingleItem;
import org.halalfoursome.kebabeditor.utils.FileChooser;
import org.halalfoursome.kebabeditor.utils.FileChooser.FileChooserMode;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.LucideIcon;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme;

public class MainWindow {
    
    private final JFrame frame;
    private final MenuBar menuBar;
    private EditorView editorView;

    static {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        FlatMTGitHubDarkIJTheme.setup();
    }

    public MainWindow(Editor editor) {
        setupStyle();

        frame = new JFrame("Kebab Editor");
        menuBar = new MenuBar(frame);

        setupLayout();
        setupActions();

        // create editor view and navigation
        editorView = new EditorView();
        frame.add(editorView, BorderLayout.CENTER);
        org.halalfoursome.kebabeditor.ui.navigation.FreeFlightNavigation nav = new org.halalfoursome.kebabeditor.ui.navigation.FreeFlightNavigation();
        editorView.setNavigationMode(nav);
    }

    public void show() {
        FocusDummy focusDummy = new FocusDummy();
        frame.add(focusDummy, BorderLayout.SOUTH);
        frame.setVisible(true);

        SwingUtilities.invokeLater(focusDummy::requestFocusInWindow);
    }

    private void setupStyle() {
        KebabStyle style = KebabStyle.getCurrent();

        // Title bar
        UIManager.put("TitlePane.iconSize", new Dimension(
            KebabStyle.ICON_WIDTH, 
            KebabStyle.ICON_HEIGHT
        ));
        UIManager.put("TitlePane.titleMargins", new Insets(12,12,12,12));
        UIManager.put("TitlePane.font", style.uiFont());

        // Tabbed panes
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("TabbedPane.tabInsets", new Insets(6, 14, 6, 14));
        UIManager.put("TabbedPane.showTabSeparators", true);

        // Focused items
        UIManager.put("Component.focusWidth", 3);
        UIManager.put("Component.focusColor", new Color(172, 108, 64, 172));

        // File chooser
        UIManager.put("FileChooser.font", style.uiFont());
        UIManager.put("FileChooser.listFont", style.uiFont());
        UIManager.put("FileChooser.textFont", style.uiFont());
        UIManager.put("FileChooser.buttonFont", style.uiFont());
        UIManager.put("FileChooser.labelFont", style.uiFont());

        // Menu
        UIManager.put("PopupMenu.borderInsets", new Insets(6, 0, 6, 0));
    }

    private void setupLayout() {
        KebabStyle style = KebabStyle.getCurrent();
        
        // Main frame properties
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(style.windowSize().width, style.windowSize().height);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(style.favicon().getImage());
        frame.setLayout(new BorderLayout());

        // Menu bar
        menuBar.addMenu("File", fileMenu());
        menuBar.addMenu("Edit", new MenuItem[] {});
        menuBar.addMenu("View", viewMenu());
        menuBar.addMenu("Options", optionsMenu());
        menuBar.addMenu("Help", new MenuItem[] {});

        // Glue between menu bar and buttons
        menuBar.add(Box.createGlue());

        // Menu buttons
        menuBar.add(new MenuButton(
            LucideIcon.PLAY, 
            "Run level", 
            MenuButton.PLAY_BUTTON_COLOR, 
            () -> {
                // TODO: Run created level
            }
        ));

        menuBar.add(new MenuButton(
            LucideIcon.ELLIPSIS_VERTICAL, 
            "More...", 
            new Insets(8, 2, 8, 2), 
            () -> {
                // TODO: More...
            }
        ));
    }

    private MenuItem[] fileMenu() {
        return new MenuItem[] {
            new SingleItem("New file...", Optional.of("control N"), () -> {
                CreateProjectDialog createProjectDialog = new CreateProjectDialog(frame);
                createProjectDialog.setVisible(true);
            }),

            new SeparatorItem(),

            new SingleItem("Open file", Optional.of("control O"), () -> {
                FileChooser fileChooser = new FileChooser(FileChooserMode.FILES);
                fileChooser.open(frame, file -> {
                    // TODO: Open file
                });
            }),

            new SeparatorItem(),

            new SingleItem("Save", Optional.of("control S"), () -> {
                // TODO: Save file
            }),

            new SeparatorItem(),

            new SingleItem("Exit", Optional.of("control Q"), () -> {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }),
        };
    }

    private MenuItem[] viewMenu() {
        return new MenuItem[] {
            new SingleItem("Blockout view", Optional.of("control V B"), () -> {

            }),

            new SeparatorItem(),
        };
    }

    private MenuItem[] optionsMenu() {
        return new MenuItem[] {
            new SingleItem("Blockout view options", Optional.of("control O B"), () -> {
                OptionsDialog optionsDialog = new OptionsDialog(frame);
                optionsDialog.setVisible(true);
            }),

            new SeparatorItem(),
        };
    }

    private void setupActions() {}
}
