package org.halalfoursome.kebabeditor.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Ray;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.render.Camera;
import org.halalfoursome.kebabeditor.render.DrawableRenderer;
import org.halalfoursome.kebabeditor.render.GizmoMode;
import org.halalfoursome.kebabeditor.render.GizmoRenderer;
import org.halalfoursome.kebabeditor.render.GreyboxRenderer;
import org.halalfoursome.kebabeditor.render.ArchetypeRenderer;
import org.halalfoursome.kebabeditor.render.GizmoInteraction;
import org.halalfoursome.kebabeditor.render.PickHit;
import org.halalfoursome.kebabeditor.render.Picker;
import org.halalfoursome.kebabeditor.render.PlacementRenderer;
import org.halalfoursome.kebabeditor.render.GridRenderer;
import org.halalfoursome.kebabeditor.render.RenderContext;
import org.halalfoursome.kebabeditor.render.RenderPass;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.ui.navigation.NavigationMode;
import org.halalfoursome.kebabeditor.ui.placement.PlacementTool;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

/**
 * The 3D viewport. A lightweight {@link GLJPanel}, so Swing popups and split
 * panes draw over it correctly.
 *
 * Frames are driven by a Swing timer and rendered synchronously on the EDT,
 * which means navigation, scene edits and rendering never race each other.
 *
 * A left click (press and release without dragging) selects what is under the
 * cursor, or places an object while the {@link PlacementTool} is active.
 * Pressing on a gizmo handle drags it instead; 1, 2 and 3 switch the gizmo
 * between move, rotate and scale.
 */
public class EditorView extends GLJPanel implements GLEventListener, GizmoInteraction.Viewport {

    private static final int FRAME_MILLIS = 16;
    private static final float[] CLEAR_COLOR = { 28 / 255f, 30 / 255f, 34 / 255f };
    private static final float SPAWN_DISTANCE = 8f;
    private static final float MAX_GROUND_DISTANCE = 60f;
    private static final int CLICK_SLOP_PIXELS = 4;

    private final Editor editor;
    private final Camera camera = new Camera();
    private final DrawableRenderer drawableObjects = new DrawableRenderer();
    private final GizmoRenderer gizmo = new GizmoRenderer();
    private final GizmoInteraction gizmoInteraction;
    private final Picker picker = new Picker();
    private final PlacementTool placement;
    private final List<Runnable> gizmoModeListeners = new ArrayList<>();
    private final List<RenderPass> passes = new ArrayList<>();
    private final Set<RenderPass> failedPasses = new HashSet<>();
    private final Timer timer;

    private NavigationMode navigationMode;
    private long lastTime;
    private boolean glReady;
    private Maybe<Point> pressedAt = Maybe.none();
    private Maybe<Point> gizmoPressedAt = Maybe.none();
    private final List<Consumer<SceneNode>> nodeActivatedListeners = new ArrayList<>();

    public EditorView(Editor editor) {
        super(capabilities());
        this.editor = editor;

        setPreferredSize(new Dimension(800, 600));
        setMinimumSize(new Dimension(100, 100));
        setFocusable(true);

        // Order matters: opaque scene geometry, then the translucent grid and placement
        // ghost over it, then the gizmo, which clears depth to stay on top.
        passes.add(new GreyboxRenderer(drawableObjects));
        passes.add(new ArchetypeRenderer(drawableObjects));
        passes.add(new GridRenderer());
        passes.add(new PlacementRenderer(drawableObjects));
        passes.add(gizmo);

        placement = new PlacementTool(this, editor, picker);
        gizmoInteraction = new GizmoInteraction(gizmo, editor::snapStep);
        // Registered before any navigation mode, so these listeners see events first.
        installInput();

        addGLEventListener(this);

        lastTime = System.nanoTime();
        timer = new Timer(FRAME_MILLIS, e -> tick());
        timer.start();
    }

    private static GLCapabilities capabilities() {
        GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL3));
        caps.setDepthBits(24);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        return caps;
    }

    public Camera getCamera() {
        return camera;
    }

    @Override
    public Camera camera() {
        return camera;
    }

    public PlacementTool getPlacement() {
        return placement;
    }

    /** The ray from the eye through a point of this component, in component pixels. */
    @Override
    public Ray rayAt(int x, int y) {
        float width = Math.max(getWidth(), 1);
        float height = Math.max(getHeight(), 1);
        return camera.ray(2f * x / width - 1f, 1f - 2f * y / height, width / height);
    }

    @Override
    public Maybe<Point2D.Float> project(Vec3 world) {
        float width = Math.max(getWidth(), 1);
        float height = Math.max(getHeight(), 1);
        Mat4 viewProjection = camera.projection(width / height).mul(camera.view());
        float[] m = viewProjection.toArray();

        float x = m[0] * world.getX() + m[4] * world.getY() + m[8] * world.getZ() + m[12];
        float y = m[1] * world.getX() + m[5] * world.getY() + m[9] * world.getZ() + m[13];
        float w = m[3] * world.getX() + m[7] * world.getY() + m[11] * world.getZ() + m[15];
        if (w <= 1e-5f) {
            return Maybe.none();
        }
        return Maybe.some(new Point2D.Float((x / w + 1f) / 2f * width, (1f - y / w) / 2f * height));
    }

    /** What archetype behaviors need to draw and pick: the project's model resolver and scene file. */
    public Maybe<BehaviorContext> behaviorContext() {
        return editor.modelResolver().andThen(models ->
            editor.prefabResolver().andThen(prefabs ->
                editor.currentProjectFile().map(file -> new BehaviorContext(models, prefabs, file))));
    }

    /**
     * Where a new object should go: the point on the ground the camera looks at,
     * or a point a short way ahead when the ground is out of sight. Snapped to
     * the grid while snapping is on.
     */
    public Vec3 spawnPoint() {
        Vec3 eye = camera.getPosition();
        Vec3 dir = camera.forward();
        float distance = SPAWN_DISTANCE;
        boolean onGround = false;

        if (Math.abs(dir.getY()) > 1e-4f) {
            float t = -eye.getY() / dir.getY();
            if (t > 0 && t < MAX_GROUND_DISTANCE) {
                distance = t;
                onGround = true;
            }
        }

        Vec3 point = eye.add(dir.mulLocal(distance));
        if (onGround) {
            point.setY(0f);
        }
        editor.snapStep().ifSome(step -> point.set(
            snap(point.getX(), step),
            snap(point.getY(), step),
            snap(point.getZ(), step)
        ));
        return point;
    }

    private static float snap(float value, float step) {
        return Math.round(value / step) * step;
    }

    private void installInput() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressedAt = Maybe.none();
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                // A press on a gizmo handle starts a drag, which the camera must not see.
                if (!placement.isActive()
                    && gizmoInteraction.press(EditorView.this, editor.selection().primary(), e.getX(), e.getY())) {
                    requestFocusInWindow();
                    gizmoPressedAt = Maybe.some(e.getPoint());
                    e.consume();
                    return;
                }
                pressedAt = Maybe.some(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (gizmoInteraction.release()) {
                    boolean doubleClick = e.getClickCount() >= 2
                        && gizmoPressedAt.isSomeAnd(p -> p.distance(e.getPoint()) <= CLICK_SLOP_PIXELS);
                    gizmoPressedAt = Maybe.none();
                    if (doubleClick) {
                        editor.selection().primary().ifSome(EditorView.this::activate);
                    }
                    e.consume();
                    return;
                }
                boolean click = pressedAt.isSomeAnd(p -> p.distance(e.getPoint()) <= CLICK_SLOP_PIXELS);
                pressedAt = Maybe.none();
                if (click && SwingUtilities.isLeftMouseButton(e)) {
                    clicked(e.getX(), e.getY(), e.getClickCount());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                placement.cursorLeft();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                placement.cursorMoved(e.getX(), e.getY());
                placement.setSurfacesIgnored(e.isAltDown());
                if (!placement.isActive()) {
                    gizmoInteraction.hover(EditorView.this, editor.selection().primary(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (gizmoInteraction.isDragging()) {
                    gizmoInteraction.drag(EditorView.this, e.getX(), e.getY());
                    e.consume();
                    return;
                }
                placement.cursorMoved(e.getX(), e.getY());
                placement.setSurfacesIgnored(e.isAltDown());
            }
        });

        // While placing, the wheel sets the ghost's distance instead of moving the camera.
        addMouseWheelListener(e -> {
            if (placement.isActive()) {
                placement.wheel(e.getPreciseWheelRotation());
                e.consume();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && gizmoInteraction.isDragging()) {
                    gizmoInteraction.cancel();
                    e.consume();
                    return;
                }
                if (!e.isControlDown() && !e.isAltDown() && !gizmoInteraction.isDragging()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_1, KeyEvent.VK_NUMPAD1 -> setGizmoMode(GizmoMode.TRANSLATE);
                        case KeyEvent.VK_2, KeyEvent.VK_NUMPAD2 -> setGizmoMode(GizmoMode.ROTATE);
                        case KeyEvent.VK_3, KeyEvent.VK_NUMPAD3 -> setGizmoMode(GizmoMode.SCALE);
                        default -> { }
                    }
                }
                if (!placement.isActive()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    placement.setActive(false);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    placement.setSurfacesIgnored(true);
                    e.consume();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    placement.setSurfacesIgnored(false);
                    if (placement.isActive()) {
                        e.consume();
                    }
                }
            }
        });
    }

    private void clicked(int x, int y, int clickCount) {
        if (placement.isActive()) {
            placement.place();
            return;
        }

        if (editor.scene().isNone()) {
            return;
        }

        Maybe<SceneNode> node = picker.pick(editor.scene().unwrap(), behaviorContext(), rayAt(x, y), false)
            .andThen(PickHit::node);

        node.ifSomeOrElse(hit -> {
            editor.selection().select(hit);
            if (clickCount >= 2) {
                activate(hit);
            }
        }, editor.selection()::clear);
    }

    private void activate(SceneNode node) {
        SwingUtilities.invokeLater(() -> nodeActivatedListeners.forEach(listener -> listener.accept(node)));
    }

    public void addNodeActivatedListener(Consumer<SceneNode> listener) {
        nodeActivatedListeners.add(listener);
    }

    public GizmoMode getGizmoMode() {
        return gizmo.getMode();
    }

    public void setGizmoMode(GizmoMode mode) {
        if (gizmo.getMode() == mode) {
            return;
        }
        gizmoInteraction.cancel();
        gizmo.setMode(mode);
        gizmoModeListeners.forEach(Runnable::run);
    }

    /** Called when the gizmo mode changes, e.g. to keep menu items in sync. */
    public void addGizmoModeListener(Runnable listener) {
        gizmoModeListeners.add(listener);
    }

    public void setNavigationMode(NavigationMode mode) {
        if (navigationMode != null) navigationMode.detach();
        navigationMode = mode;
        if (navigationMode != null) navigationMode.attach(this);
        requestFocusInWindow();
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastTime) / 1e9;
        lastTime = now;

        if (navigationMode != null) navigationMode.update(dt);
        display();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();
        LogService.get().info("OpenGL " + gl.glGetString(GL3.GL_VERSION)
            + " on " + gl.glGetString(GL3.GL_RENDERER));

        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL3.GL_LEQUAL);
        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_BACK);
        gl.glEnable(GL3.GL_MULTISAMPLE);

        failedPasses.clear();
        try {
            drawableObjects.init(gl);
        } catch (RuntimeException e) {
            LogService.get().error("Viewport: mesh shader failed to initialize", e);
        }
        for (RenderPass pass : passes) {
            try {
                pass.init(gl);
            } catch (RuntimeException e) {
                fail(pass, "initialize", e);
            }
        }

        glReady = true;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        if (!glReady) {
            return;
        }

        GL3 gl = drawable.getGL().getGL3();
        gl.glClearColor(CLEAR_COLOR[0], CLEAR_COLOR[1], CLEAR_COLOR[2], 1f);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        RenderContext context = RenderContext.of(
            camera,
            drawable.getSurfaceWidth(),
            drawable.getSurfaceHeight(),
            editor.scene(),
            editor.currentProjectFile(),
            behaviorContext(),
            editor.selection().nodes(),
            placement.preview()
        );

        for (RenderPass pass : passes) {
            if (failedPasses.contains(pass)) {
                continue;
            }
            try {
                pass.render(gl, context);
            } catch (RuntimeException e) {
                fail(pass, "render", e);
            }
        }

        drawableObjects.endFrame(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // JOGL already updates glViewport; projection is rebuilt from the size every frame.
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();
        glReady = false;

        for (RenderPass pass : passes.reversed()) {
            try {
                pass.dispose(gl);
            } catch (RuntimeException e) {
                LogService.get().error("Failed to dispose " + pass.getClass().getSimpleName(), e);
            }
        }
        drawableObjects.dispose(gl);
    }

    // A broken pass is disabled instead of logging the same error every frame.
    private void fail(RenderPass pass, String stage, RuntimeException e) {
        failedPasses.add(pass);
        LogService.get().error(
            "Viewport: " + pass.getClass().getSimpleName() + " failed to " + stage + " and was disabled",
            e
        );
    }
}
