package org.halalfoursome.kebabeditor.math;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuatEulerTest {
    @Test void inverseUsesYXZOrderIncludingSingularitiesAndEquivalentSigns() {
        Random random = new Random(71);
        for (int i=0;i<2000;i++) {
            float pitch = i<2 ? (float)Math.copySign(Math.PI/2, i==0?1:-1) : (random.nextFloat()-.5f)*6.28f;
            Quat q = Quat.fromEuler(pitch, (random.nextFloat()-.5f)*12, (random.nextFloat()-.5f)*12);
            Vec3 angles=q.toEuler();
            Quat reconstructed=Quat.fromEuler(angles.getX(),angles.getY(),angles.getZ());
            for (Vec3 axis : new Vec3[]{Vec3.unitX(),Vec3.unitY(),Vec3.unitZ()}) {
                Vec3 before=q.rotate(axis), after=reconstructed.rotate(axis);
                assertEquals(before.getX(),after.getX(),2e-5);
                assertEquals(before.getY(),after.getY(),2e-5);
                assertEquals(before.getZ(),after.getZ(),2e-5);
            }
            Vec3 opposite = new Quat(-q.getX(),-q.getY(),-q.getZ(),-q.getW()).toEuler();
            assertEquals(angles,opposite);
        }
    }
}
