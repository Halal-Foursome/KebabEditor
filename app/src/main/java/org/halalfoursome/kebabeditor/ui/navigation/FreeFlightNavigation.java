package org.halalfoursome.kebabeditor.ui.navigation;
 
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;
import java.util.Set;
 
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.render.Camera;
import org.halalfoursome.kebabeditor.ui.EditorView;
 
public class FreeFlightNavigation implements NavigationMode {
 
    private static final double LOOK_DEGREES_PER_PIXEL = 0.2;
    private static final float WHEEL_STEP = 1.5f;
    // Ctrl doubles as "fly down" and the shortcut modifier: it only flies once held
    // alone this long, so Ctrl+B, Ctrl+S, ... don't nudge the camera.
    private static final long CTRL_ALONE_NANOS = 250_000_000L;
 
    private EditorView view;
    private final Set<Integer> keys = new HashSet<>();
    private int lastMouseX, lastMouseY;
    private boolean dragging = false;
 
    private long ctrlPressedAt;
    private boolean ctrlUsedForShortcut;

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_CONTROL) {
                if (keys.add(code)) {
                    ctrlPressedAt = System.nanoTime();
                    ctrlUsedForShortcut = false;
                }
                return;
            }
            if (e.isControlDown() || e.isAltDown()) {
                // Part of a shortcut, not movement.
                ctrlUsedForShortcut |= e.isControlDown();
                return;
            }
            keys.add(code);
        }

        @Override
        public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
    };

    // Releases never arrive while a dialog or menu has focus, so forget held keys.
    private final FocusAdapter focusAdapter = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) { keys.clear(); }
    };
 
    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isConsumed()) return; // e.g. the press grabbed a gizmo handle
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
            if (!dragging || e.isConsumed()) return;
            int dx = e.getX() - lastMouseX;
            int dy = e.getY() - lastMouseY;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
 
            // rotate camera: drag right turns right, drag up looks up
            view.getCamera().rotate(
                (float) -Math.toRadians(dx * LOOK_DEGREES_PER_PIXEL),
                (float) -Math.toRadians(dy * LOOK_DEGREES_PER_PIXEL)
            );
        }
    };
 
    private final MouseWheelListener wheelListener = new MouseWheelListener() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isConsumed()) return; // e.g. the placement tool is using the wheel
            // dolly along the view direction; wheel towards the user moves back
            Camera cam = view.getCamera();
            cam.move(cam.forward().mulLocal((float) -e.getPreciseWheelRotation() * WHEEL_STEP));
        }
    };
 
    @Override
    public void attach(EditorView view) {
        this.view = view;
        view.addKeyListener(keyAdapter);
        view.addMouseListener(mouseAdapter);
        view.addMouseMotionListener(motionAdapter);
        view.addMouseWheelListener(wheelListener);
        view.addFocusListener(focusAdapter);
    }
 
    @Override
    public void detach() {
        if (view == null) return;
        view.removeKeyListener(keyAdapter);
        view.removeMouseListener(mouseAdapter);
        view.removeMouseMotionListener(motionAdapter);
        view.removeMouseWheelListener(wheelListener);
        view.removeFocusListener(focusAdapter);
        keys.clear();
        view = null;
    }
 
    private boolean keyDown(int... codes) {
        for (int c : codes) if (keys.contains(c)) return true;
        return false;
    }
 
    private boolean ctrlHeldAlone() {
        return keys.contains(KeyEvent.VK_CONTROL)
            && !ctrlUsedForShortcut
            && System.nanoTime() - ctrlPressedAt >= CTRL_ALONE_NANOS;
    }
 
    @Override
    public void update(double deltaSeconds) {
        if (view == null) return;
        Camera cam = view.getCamera();
        double moveSpeed = 10.0; // units per second
        if (keys.contains(KeyEvent.VK_SHIFT)) moveSpeed *= 3;
 
        float fwdInput = 0, rightInput = 0, upInput = 0;
        // W moves forward along camera forward vector, S moves backward
        if (keys.contains(KeyEvent.VK_W)) fwdInput += 1;
        if (keys.contains(KeyEvent.VK_S)) fwdInput -= 1;
 
        // D / Numpad6 -> right, A / Numpad4 -> left
        if (keyDown(KeyEvent.VK_D, KeyEvent.VK_NUMPAD6, KeyEvent.VK_KP_RIGHT)) rightInput += 1;
        if (keyDown(KeyEvent.VK_A, KeyEvent.VK_NUMPAD4, KeyEvent.VK_KP_LEFT)) rightInput -= 1;
 
        // Space / Numpad9 -> up, Ctrl / Numpad2 -> down
        if (keyDown(KeyEvent.VK_SPACE, KeyEvent.VK_NUMPAD9)) upInput += 1;
        if (ctrlHeldAlone() || keyDown(KeyEvent.VK_NUMPAD2, KeyEvent.VK_KP_DOWN)) upInput -= 1;
 
        Vec3 delta = cam.forward().mulLocal(fwdInput)
            .addLocal(cam.right().mulLocal(rightInput))
            .addLocal(0, upInput, 0);
 
        cam.move(delta.mulLocal((float) (moveSpeed * deltaSeconds)));
    }
}
