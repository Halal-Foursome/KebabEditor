package org.halalfoursome.kebabeditor.ui.playtest;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.render.Camera;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.ui.navigation.NavigationMode;

/**
 * Plays the level: WASD or the arrow keys run relative to the camera, Space
 * jumps, dragging the mouse orbits a third-person camera around the actor and
 * the wheel zooms it. Esc ends the playtest.
 *
 * The camera stops short of solids between it and the actor, so walls do not
 * hide the character.
 */
public class PlaytestNavigation implements NavigationMode {

    private static final double LOOK_DEGREES_PER_PIXEL = 0.25;
    private static final float START_PITCH = (float) Math.toRadians(-20);
    private static final float MIN_PITCH = (float) Math.toRadians(-80);
    private static final float MAX_PITCH = (float) Math.toRadians(30);
    private static final float START_DISTANCE = 6f;
    private static final float MIN_DISTANCE = 1.5f;
    private static final float MAX_DISTANCE = 20f;
    private static final float WHEEL_STEP = 0.8f;
    /** Gap kept between the camera and a solid it would otherwise sit in. */
    private static final float CLEARANCE = 0.25f;
    /** The camera looks at this fraction of the actor's height. */
    private static final float LOOK_AT_HEIGHT = 0.85f;

    private final PlaytestSession session;
    private final Runnable stop;
    private final Consumer<RuntimeException> failed;
    private final Set<Integer> keys = new HashSet<>();

    private EditorView view;
    private float yaw;
    private float pitch = START_PITCH;
    private float distance = START_DISTANCE;
    private boolean jumpPressed;
    private boolean dragging;
    private int lastMouseX;
    private int lastMouseY;

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_ESCAPE) {
                e.consume();
                stop.run();
                return;
            }
            if (e.isControlDown() || e.isAltDown()) {
                return; // a shortcut, not movement
            }
            // Held keys repeat; only the first press of Space is a jump.
            if (keys.add(code) && code == KeyEvent.VK_SPACE) {
                jumpPressed = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keys.remove(e.getKeyCode());
        }
    };

    // Releases never arrive while a dialog or menu has focus, so forget held keys.
    private final FocusAdapter focusAdapter = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
            keys.clear();
            dragging = false;
        }
    };

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            dragging = true;
            view.requestFocusInWindow();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
        }
    };

    private final MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragging) {
                return;
            }
            int dx = e.getX() - lastMouseX;
            int dy = e.getY() - lastMouseY;
            lastMouseX = e.getX();
            lastMouseY = e.getY();

            yaw -= (float) Math.toRadians(dx * LOOK_DEGREES_PER_PIXEL);
            pitch = Math.clamp(pitch - (float) Math.toRadians(dy * LOOK_DEGREES_PER_PIXEL), MIN_PITCH, MAX_PITCH);
        }
    };

    private final MouseWheelListener wheelListener = e ->
        distance = Math.clamp(distance + (float) e.getPreciseWheelRotation() * WHEEL_STEP, MIN_DISTANCE, MAX_DISTANCE);

    /**
     * @param stop   ends the playtest, e.g. when Esc is pressed
     * @param failed ends it after the simulation threw
     */
    public PlaytestNavigation(PlaytestSession session, Runnable stop, Consumer<RuntimeException> failed) {
        this.session = session;
        this.stop = stop;
        this.failed = failed;
        this.yaw = session.actorYaw();
    }

    @Override
    public void attach(EditorView view) {
        this.view = view;
        view.addKeyListener(keyAdapter);
        view.addMouseListener(mouseAdapter);
        view.addMouseMotionListener(motionAdapter);
        view.addMouseWheelListener(wheelListener);
        view.addFocusListener(focusAdapter);
        placeCamera();
    }

    @Override
    public void detach() {
        if (view == null) {
            return;
        }
        view.removeKeyListener(keyAdapter);
        view.removeMouseListener(mouseAdapter);
        view.removeMouseMotionListener(motionAdapter);
        view.removeMouseWheelListener(wheelListener);
        view.removeFocusListener(focusAdapter);
        keys.clear();
        view = null;
    }

    @Override
    public void update(double deltaSeconds) {
        if (view == null) {
            return;
        }

        try {
            session.advance(deltaSeconds, moveInput(), jumpPressed);
        } catch (RuntimeException e) {
            failed.accept(e);
            return;
        } finally {
            jumpPressed = false;
        }

        placeCamera();
    }

    // Forward is where the camera looks, flattened onto the ground.
    private Vec3 moveInput() {
        float forward = axis(KeyEvent.VK_W, KeyEvent.VK_UP) - axis(KeyEvent.VK_S, KeyEvent.VK_DOWN);
        float right = axis(KeyEvent.VK_D, KeyEvent.VK_RIGHT) - axis(KeyEvent.VK_A, KeyEvent.VK_LEFT);

        Vec3 move = new Vec3((float) -Math.sin(yaw), 0, (float) -Math.cos(yaw)).mulLocal(forward)
            .addLocal(new Vec3((float) Math.cos(yaw), 0, (float) -Math.sin(yaw)).mulLocal(right));

        return move.lengthSquared() > 1 ? move.normalizeLocal() : move;
    }

    private float axis(int key, int alternative) {
        return keys.contains(key) || keys.contains(alternative) ? 1f : 0f;
    }

    private void placeCamera() {
        Camera camera = view.getCamera();
        camera.setOrientation(yaw, pitch);

        Vec3 target = session.actorPosition().addLocal(0, session.settings().height() * LOOK_AT_HEIGHT, 0);
        Vec3 back = camera.forward().negateLocal();
        float reach = session.world().raycast(target, back, distance + CLEARANCE)
            .map(hit -> Math.max(hit - CLEARANCE, 0.1f))
            .unwrapOr(distance);

        camera.getPosition().set(target.add(back.mulLocal(Math.min(reach, distance))));
    }
}
