package org.halalfoursome.kebabeditor.ui.navigation;
 
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;
import java.util.Set;
 
import org.halalfoursome.kebabeditor.ui.EditorView;
 
public class FreeFlightNavigation implements NavigationMode {
 
    private EditorView view;
    private final Set<Integer> keys = new HashSet<>();
    private int lastMouseX, lastMouseY;
    private boolean dragging = false;
 
    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) { keys.add(e.getKeyCode()); }
        @Override
        public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
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
            if (!dragging) return;
            int dx = e.getX() - lastMouseX;
            int dy = e.getY() - lastMouseY;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
 
            // rotate camera
            EditorView.Camera cam = view.getCamera();
            cam.yaw -= Math.toRadians(dx * 0.2);
            cam.pitch -= Math.toRadians(dy * 0.2);
            if (cam.pitch > Math.toRadians(89)) cam.pitch = Math.toRadians(89);
            if (cam.pitch < Math.toRadians(-89)) cam.pitch = Math.toRadians(-89);
        }
    };
 
    private final MouseWheelListener wheelListener = new MouseWheelListener() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            EditorView.Camera cam = view.getCamera();
            cam.y += e.getWheelRotation();
        }
    };
 
    @Override
    public void attach(EditorView view) {
        this.view = view;
        view.addKeyListener(keyAdapter);
        view.addMouseListener(mouseAdapter);
        view.addMouseMotionListener(motionAdapter);
        view.addMouseWheelListener(wheelListener);
    }
 
    @Override
    public void detach() {
        if (view == null) return;
        view.removeKeyListener(keyAdapter);
        view.removeMouseListener(mouseAdapter);
        view.removeMouseMotionListener(motionAdapter);
        view.removeMouseWheelListener(wheelListener);
        view = null;
    }
 
    private boolean keyDown(int... codes) {
        for (int c : codes) if (keys.contains(c)) return true;
        return false;
    }
 
    @Override
    public void update(double deltaSeconds) {
        if (view == null) return;
        EditorView.Camera cam = view.getCamera();
        double moveSpeed = 10.0; // units per second
        if (keys.contains(KeyEvent.VK_SHIFT)) moveSpeed *= 3;
 
        double fwdInput = 0, rightInput = 0, upInput = 0;
        // W moves forward along camera forward vector, S moves backward
        if (keys.contains(KeyEvent.VK_W)) fwdInput += 1;
        if (keys.contains(KeyEvent.VK_S)) fwdInput -= 1;
 
        // D / Numpad6 -> right, A / Numpad4 -> left
        if (keyDown(KeyEvent.VK_D, KeyEvent.VK_NUMPAD6, KeyEvent.VK_KP_RIGHT)) rightInput += 1;
        if (keyDown(KeyEvent.VK_A, KeyEvent.VK_NUMPAD4, KeyEvent.VK_KP_LEFT)) rightInput -= 1;
 
        // Space / Numpad9 -> up, Ctrl / Numpad2 -> down
        if (keyDown(KeyEvent.VK_SPACE, KeyEvent.VK_NUMPAD9)) upInput += 1;
        if (keyDown(KeyEvent.VK_CONTROL, KeyEvent.VK_NUMPAD2, KeyEvent.VK_KP_DOWN)) upInput -= 1;
 
        // Forward/right vectors MUST match the rotation actually applied in
        // EditorView.worldToCamera(), which does R_pitch(-pitch) * R_yaw(-yaw).
        // Solving that transform for "straight ahead" / "purely sideways"
        // gives the formulas below (verified: for these vectors, camera-space
        // Rx/Ry come out as 0, independent of yaw/pitch).
        double cosP = Math.cos(cam.pitch);
        double sinP = Math.sin(cam.pitch);
        double cosY = Math.cos(cam.yaw);
        double sinY = Math.sin(cam.yaw);
 
        double fx = -cosP * sinY;
        double fy = -sinP;
        double fz = cosP * cosY;
 
        // Right vector for this renderer's yaw/pitch composition is always
        // horizontal (pitch-independent) and already unit length.
        double rx = cosY;
        double ry = 0;
        double rz = sinY;
 
        double dx = (fx * fwdInput + rx * rightInput) * moveSpeed * deltaSeconds;
        double dy = (fy * fwdInput + upInput) * moveSpeed * deltaSeconds;
        double dz = (fz * fwdInput + rz * rightInput) * moveSpeed * deltaSeconds;
 
        cam.x += dx;
        cam.y += dy;
        cam.z += dz;
    }
}
