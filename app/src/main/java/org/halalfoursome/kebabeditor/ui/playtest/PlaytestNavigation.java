package org.halalfoursome.kebabeditor.ui.playtest;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.function.Consumer;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.ui.navigation.NavigationMode;

/**
 * Plays the level in the viewport: feeds the keys to {@link PlaytestKeys},
 * mouse drags and the wheel to a {@link ThirdPersonRig}, and each frame advances
 * the session and places the camera. Esc ends the playtest.
 */
public class PlaytestNavigation implements NavigationMode {

    /** The camera looks at this fraction of the actor's height. */
    private static final float LOOK_AT_HEIGHT = 0.85f;

    private final PlaytestSession session;
    private final Runnable stop;
    private final Consumer<RuntimeException> failed;
    private final PlaytestKeys keys = new PlaytestKeys();
    private final ThirdPersonRig rig;

    private EditorView view;
    private boolean dragging;
    private int lastMouseX;
    private int lastMouseY;

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                e.consume();
                stop.run();
                return;
            }
            if (e.isControlDown() || e.isAltDown()) {
                return; // a shortcut, not movement
            }
            keys.press(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keys.release(e.getKeyCode());
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
            rig.orbit(e.getX() - lastMouseX, e.getY() - lastMouseY);
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        }
    };

    private final MouseWheelListener wheelListener = this::wheelMoved;

    /**
     * @param stop   ends the playtest, e.g. when Esc is pressed
     * @param failed ends it after the simulation threw
     */
    public PlaytestNavigation(PlaytestSession session, Runnable stop, Consumer<RuntimeException> failed) {
        this.session = session;
        this.stop = stop;
        this.failed = failed;
        this.rig = new ThirdPersonRig(session.actorYaw());
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
            session.advance(deltaSeconds, keys.move(rig.yaw()), keys.takeJump());
        } catch (RuntimeException e) {
            failed.accept(e);
            return;
        }

        placeCamera();
    }

    private void wheelMoved(MouseWheelEvent e) {
        rig.zoom(e.getPreciseWheelRotation());
    }

    private void placeCamera() {
        Vec3 target = session.actorPosition().addLocal(0, session.settings().height() * LOOK_AT_HEIGHT, 0);
        rig.place(view.getCamera(), target, session.world());
    }
}
