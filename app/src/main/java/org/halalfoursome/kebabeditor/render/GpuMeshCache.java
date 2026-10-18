package org.halalfoursome.kebabeditor.render;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.halalfoursome.kebabeditor.model.MeshData;

import com.jogamp.opengl.GL3;

/**
 * Uploads each {@link MeshData} once, keyed by identity. Meshes not drawn for a
 * while (e.g. from a closed project) are freed by {@link #evictUnused}.
 */
public final class GpuMeshCache {

    private static final long EVICT_AFTER_FRAMES = 600;

    private final Map<MeshData, Entry> entries = new IdentityHashMap<>();
    private long frame;

    private static final class Entry {
        final GpuMesh mesh;
        long lastUsed;

        Entry(GpuMesh mesh) {
            this.mesh = mesh;
        }
    }

    public GpuMesh get(GL3 gl, MeshData mesh) {
        Entry entry = entries.computeIfAbsent(mesh, m -> new Entry(GpuMesh.upload(gl, m)));
        entry.lastUsed = frame;
        return entry.mesh;
    }

    /** Call once per frame, after all passes have drawn. */
    public void evictUnused(GL3 gl) {
        Iterator<Entry> it = entries.values().iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (frame - entry.lastUsed > EVICT_AFTER_FRAMES) {
                entry.mesh.dispose(gl);
                it.remove();
            }
        }
        frame++;
    }

    public void dispose(GL3 gl) {
        entries.values().forEach(entry -> entry.mesh.dispose(gl));
        entries.clear();
    }
}
