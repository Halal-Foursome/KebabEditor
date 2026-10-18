package org.halalfoursome.kebabeditor.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.BehaviorProblem;
import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.scene.model.Scene;

import com.jogamp.opengl.GL3;

/**
 * Archetype instances that know how to draw themselves (model_ref, colliders,
 * triggers, snap_distance, ...): each one's {@code ArchetypeBehavior} supplies
 * the drawables, and its selection shape outlines it while selected.
 *
 * Behavior problems (a broken model path, an invalid size) are logged once
 * when they appear rather than every frame.
 */
public class ArchetypeRenderer implements RenderPass {

    private static final int VALIDATE_EVERY_FRAMES = 60;
    private static final Material OUTLINE = Material.solid(Palette.SELECTION);

    private final DrawableRenderer drawables;
    private final Set<String> reported = new HashSet<>();
    private Scene reportedFor;
    private int frame;

    public ArchetypeRenderer(DrawableRenderer drawables) {
        this.drawables = drawables;
    }

    @Override
    public void init(GL3 gl) {
        // Uses the shared DrawableRenderer, which the viewport initializes.
    }

    @Override
    public void render(GL3 gl, RenderContext context) {
        if (context.scene().isNone() || context.behaviors().isNone()) {
            return;
        }

        Scene scene = context.scene().unwrap();
        BehaviorContext behaviors = context.behaviors().unwrap();
        boolean validate = frame++ % VALIDATE_EVERY_FRAMES == 0;
        if (scene != reportedFor) {
            reported.clear();
            reportedFor = scene;
        }

        List<Drawable> objects = new ArrayList<>();
        List<Drawable> outlines = new ArrayList<>();

        SceneArchetypes.of(scene).forEach(entry -> {
            objects.addAll(entry.behavior().drawableObjects(entry.node(), entry.archetype(), behaviors));

            if (context.isSelected(entry.node())) {
                entry.behavior().gizmoSelectionShape(entry.node(), entry.archetype(), behaviors)
                    .ifSome(shape -> {
                        SelectionShapes.Placed placed = SelectionShapes.place(shape, entry.node().worldMatrix());
                        outlines.add(new Drawable(placed.outline(), placed.outlineWorld(), OUTLINE, DrawMode.OVERLAY));
                    });
            }

            if (validate) {
                for (BehaviorProblem problem : entry.behavior().validate(entry.node(), entry.archetype(), behaviors)) {
                    String message = "'" + entry.node().getName() + "': " + problem.getMessage();
                    if (reported.add(message)) {
                        LogService.get().warn(message);
                    }
                }
            }
        });

        drawables.draw(gl, context, objects, false);
        drawables.draw(gl, context, outlines, false);
    }

    @Override
    public void dispose(GL3 gl) {
        // GPU meshes are owned by the shared DrawableRenderer.
    }
}
