package org.halalfoursome.kebabeditor.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.halalfoursome.kebabeditor.ui.navigation.NavigationMode;

public class EditorView extends JPanel {

    public static class Camera {
        public double x = 0, y = 10, z = 20;
        public double yaw = Math.toRadians(180); // look towards origin
        public double pitch = Math.toRadians(-20);
        public double fov = Math.toRadians(60);
    }

    private final Camera camera = new Camera();
    private NavigationMode navigationMode;
    private final Timer timer;
    private long lastTime;

    public EditorView() {
        setBackground(new Color(28, 30, 34));
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);

        lastTime = System.nanoTime();
        timer = new Timer(16, e -> tick());
        timer.start();

        // Repaint when resized so rendering bounds update
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repaint();
            }
        });
    }

    public Camera getCamera() {
        return camera;
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
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Simple 3D projection parameters
        double f = 500; // focal length

        // draw grid on XZ plane
        g2.setColor(new Color(80, 86, 92));

        int spacing = 1; // spacing between grid lines (world units)
        int viewDistance = 200; // how far from camera (in world units) to render grid lines

        // Grid lines are anchored to world coordinates
        // only draw those within `viewDistance` of the camera.
        int camXi = (int) Math.floor(camera.x / (double) spacing);
        int camZi = (int) Math.floor(camera.z / (double) spacing);
        int range = (int) Math.ceil(viewDistance / (double) spacing);

        // Z-parallel lines (varying X) — lines lie at constant Z = k*spacing
        for (int zi = camZi - range; zi <= camZi + range; zi++) {
            int z = zi * spacing;
            drawLine3DClipped(g2, -viewDistance + camXi * spacing, 0, z, viewDistance + camXi * spacing, 0, z, w, h, f);
        }

        // X-parallel lines (varying Z) — lines lie at constant X = k*spacing
        for (int xi = camXi - range; xi <= camXi + range; xi++) {
            int x = xi * spacing;
            drawLine3DClipped(g2, x, 0, -viewDistance + camZi * spacing, x, 0, viewDistance + camZi * spacing, w, h, f);
        }

        // draw axes
        drawLine3D(g2, -10, 0, 0, 10, 0, 0, w, h, f); // X axis
        g2.setColor(new Color(200, 80, 80));
        drawLine3D(g2, 0, 0, -10, 0, 0, 10, w, h, f); // Z axis

        g2.dispose();
    }

    private void drawLine3D(Graphics2D g2, double x1, double y1, double z1, double x2, double y2, double z2, int w, int h, double f) {
        double[] p1 = projectCamera(x1, y1, z1, w, h, f);
        double[] p2 = projectCamera(x2, y2, z2, w, h, f);
        if (p1 == null || p2 == null) return;
        g2.drawLine((int) p1[0], (int) p1[1], (int) p2[0], (int) p2[1]);
    }

    private void drawLine3DClipped(Graphics2D g2, double x1, double y1, double z1, double x2, double y2, double z2, int w, int h, double f) {
        double[][] pts = projectClipped(x1, y1, z1, x2, y2, z2, w, h, f);
        if (pts == null) return;
        double[] p1 = pts[0];
        double[] p2 = pts[1];
        g2.drawLine((int) p1[0], (int) p1[1], (int) p2[0], (int) p2[1]);
    }

    // world -> camera-space
    private double[] worldToCamera(double px, double py, double pz) {
        double dx = px - camera.x;
        double dy = py - camera.y;
        double dz = pz - camera.z;

        double cy = Math.cos(-camera.yaw);
        double sy = Math.sin(-camera.yaw);
        double rx = cy * dx - sy * dz;
        double rz = sy * dx + cy * dz;

        double cp = Math.cos(-camera.pitch);
        double sp = Math.sin(-camera.pitch);
        double ry = cp * dy - sp * rz;
        double rz2 = sp * dy + cp * rz;

        return new double[] { rx, ry, rz2 };
    }

    // project a camera-space point (rx,ry,rz) to screen coordinates
    private double[] projectCamera(double rx, double ry, double rz, int w, int h, double f) {
        if (rz <= 0.0001) return null;
        double sx = w * 0.5 + (rx * f / rz);
        double sy2 = h * 0.5 - (ry * f / rz);
        return new double[] { sx, sy2 };
    }

    // clip a world-space segment against near plane and return two screen points or null
    private double[][] projectClipped(double x1, double y1, double z1, double x2, double y2, double z2, int w, int h, double f) {
        double near = 0.1;
        double[] c1 = worldToCamera(x1, y1, z1);
        double[] c2 = worldToCamera(x2, y2, z2);

        boolean in1 = c1[2] > near;
        boolean in2 = c2[2] > near;

        if (!in1 && !in2) return null;

        if (in1 && in2) {
            double[] p1 = projectCamera(c1[0], c1[1], c1[2], w, h, f);
            double[] p2 = projectCamera(c2[0], c2[1], c2[2], w, h, f);
            if (p1 == null || p2 == null) return null;
            return new double[][] { p1, p2 };
        }

        // one point is behind; compute intersection in camera space
        double t = (near - c1[2]) / (c2[2] - c1[2]); // when c1 + t*(c2-c1) has z = near
        if (t < 0) t = 0; if (t > 1) t = 1;

        double ix = c1[0] + t * (c2[0] - c1[0]);
        double iy = c1[1] + t * (c2[1] - c1[1]);
        double iz = near;

        if (in1) {
            double[] p1 = projectCamera(c1[0], c1[1], c1[2], w, h, f);
            double[] p2 = projectCamera(ix, iy, iz, w, h, f);
            if (p1 == null || p2 == null) return null;
            return new double[][] { p1, p2 };
        } else {
            double[] p1 = projectCamera(ix, iy, iz, w, h, f);
            double[] p2 = projectCamera(c2[0], c2[1], c2[2], w, h, f);
            if (p1 == null || p2 == null) return null;
            return new double[][] { p1, p2 };
        }
    }
}
