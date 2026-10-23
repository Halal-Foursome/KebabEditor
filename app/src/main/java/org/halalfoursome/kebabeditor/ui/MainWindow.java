package org.halalfoursome.kebabeditor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.ui.logs.LogsTab;
import org.halalfoursome.kebabeditor.ui.assets.AssetExplorerTab;
import org.halalfoursome.kebabeditor.ui.assets.AssetWindows;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.ui.menu.EditMenu;
import org.halalfoursome.kebabeditor.ui.object.ObjectDialog;
import org.halalfoursome.kebabeditor.ui.menu.FileMenu;
import org.halalfoursome.kebabeditor.ui.menu.PlaytestMenu;
import org.halalfoursome.kebabeditor.ui.menu.ProjectMenu;
import org.halalfoursome.kebabeditor.ui.menu.ViewMenu;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuBar;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuButton;
import org.halalfoursome.kebabeditor.ui.navigation.FreeFlightNavigation;
import org.halalfoursome.kebabeditor.ui.playtest.PlaytestController;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.LucideIcon;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme;
import com.jogamp.opengl.GLException;

public class MainWindow {

    private static final String APP_TITLE = "Kebab Editor";
    private static final String RUN_TOOLTIP = "Run level (F5)";

    private final JFrame frame;
    private final Editor editor;
    private final LogsTab logsTab;
    private final AssetExplorerTab assetExplorer;
    private final AssetWindows assetWindows;

    private final MenuBar menuBar;
    private EditorView editorView;
    private Maybe<PlaytestController> playtest = Maybe.none();

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
        logsTab = new LogsTab(LogService.get());
        assetWindows = new AssetWindows(frame, editor);
        assetExplorer = new AssetExplorerTab(editor, assetWindows.handler());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                playtest.ifSome(PlaytestController::stop);
                assetWindows.close();
                if (editorView != null) {
                    editorView.close();
                }
            }
        });

        setupLayout();

        editor.addProjectListener(this::updateTitle);
        updateTitle();
    }

    private Vec3 spawnPoint() {
        return editorView != null ? editorView.spawnPoint() : Vec3.zero();
    }


    // Falls back to an explanatory placeholder when no OpenGL 3 context can be made.
    private JComponent createViewport() {
        try {
            editorView = new EditorView(editor);
            editorView.setNavigationMode(new FreeFlightNavigation());
            editorView.addNodeActivatedListener(node -> new ObjectDialog(frame, editor, node).setVisible(true));
            playtest = Maybe.some(new PlaytestController(
                editor,
                editorView.getPlaytest(),
                (title, message) -> JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE),
                LogService.get()
            ));
            return editorView;
        } catch (GLException e) {
            LogService.get().error("OpenGL 3.x is not available, the 3D viewport is disabled", e);
            JLabel placeholder = new JLabel("3D viewport unavailable: OpenGL 3.x is not supported", SwingConstants.CENTER);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(placeholder, BorderLayout.CENTER);
            return panel;
        }
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
        UIManager.put("TabbedPane.font", style.uiFont());
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

        // 3D viewport above a resizable bottom tool window.
        JTabbedPane bottomTabs = new JTabbedPane();
        bottomTabs.addTab("Asset Explorer", assetExplorer);
        bottomTabs.addTab("Logs", logsTab);
        JSplitPane workspace = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            createViewport(), bottomTabs);
        workspace.setContinuousLayout(false);
        workspace.setResizeWeight(0.75);
        SwingUtilities.invokeLater(() -> workspace.setDividerLocation(0.75));
        frame.add(workspace, BorderLayout.CENTER);

        // Menu bar
        menuBar.addMenu(new FileMenu(frame, editor, assetExplorer::refresh));
        menuBar.addMenu(new ProjectMenu(frame, editor));
        menuBar.addMenu(new EditMenu(frame, editor, this::spawnPoint, Maybe.ofNullable(editorView)));
        menuBar.addMenu(new ViewMenu(frame, editor, Maybe.ofNullable(editorView)));
        menuBar.addMenu(new PlaytestMenu(playtest));
        menuBar.addMenu(new Menu("Help"));

        // Glue between menu bar and buttons
        menuBar.add(Box.createGlue());

        // Menu buttons
        MenuButton runButton = new MenuButton(
            LucideIcon.PLAY,
            RUN_TOOLTIP,
            MenuButton.PLAY_BUTTON_COLOR,
            () -> playtest.ifSomeOrElse(PlaytestController::toggle, () -> JOptionPane.showMessageDialog(
                frame,
                "The 3D viewport is unavailable, so the level cannot be run.",
                "Run level",
                JOptionPane.INFORMATION_MESSAGE
            ))
        );
        playtest.ifSome(controller -> controller.addListener(() -> {
            if (controller.isRunning()) {
                runButton.setAppearance(LucideIcon.SQUARE, MenuButton.STOP_BUTTON_COLOR, "Stop playtest (Esc)");
            } else {
                runButton.setAppearance(LucideIcon.PLAY, MenuButton.PLAY_BUTTON_COLOR, RUN_TOOLTIP);
            }
        }));
        menuBar.add(runButton);

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
