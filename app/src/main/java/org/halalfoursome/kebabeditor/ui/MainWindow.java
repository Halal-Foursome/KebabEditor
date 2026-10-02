package org.halalfoursome.kebabeditor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.ui.menu.FileMenu;
import org.halalfoursome.kebabeditor.ui.menu.OptionsMenu;
import org.halalfoursome.kebabeditor.ui.menu.ProjectMenu;
import org.halalfoursome.kebabeditor.ui.menu.ViewMenu;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuBar;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuButton;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.LucideIcon;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme;

public class MainWindow {

    private static final String APP_TITLE = "Kebab Editor";

    private final JFrame frame;
    private final Editor editor;

    private final MenuBar menuBar;
    @SuppressWarnings("unused")
    private EditorView editorView;

    static {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        FlatMTGitHubDarkIJTheme.setup();
    }

    public MainWindow(Editor editorInstance) {
        setupStyle();

        editor = editorInstance;

        frame = new JFrame(APP_TITLE);
        menuBar = new MenuBar(frame);

        setupLayout();

        editor.addProjectListener(this::updateTitle);

        // create editor view and navigation
        // editorView = new EditorView();
        // frame.add(editorView, BorderLayout.CENTER);
        // org.halalfoursome.kebabeditor.ui.navigation.FreeFlightNavigation nav = new org.halalfoursome.kebabeditor.ui.navigation.FreeFlightNavigation();
        // editorView.setNavigationMode(nav);
    }

    private void updateTitle() {
        String title = editor.currentProjectFile()
            .map(file -> file.getFileName() + " - " + APP_TITLE)
            .unwrapOr(APP_TITLE);

        frame.setTitle(title);
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
        menuBar.addMenu(new FileMenu(frame, editor));
        menuBar.addMenu(new ProjectMenu(frame, editor));
        menuBar.addMenu(new Menu("Edit"));
        menuBar.addMenu(new ViewMenu(frame, editor));
        menuBar.addMenu(new OptionsMenu(frame, editor));
        menuBar.addMenu(new Menu("Help"));

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
}
