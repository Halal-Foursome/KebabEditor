#version 330 core

in vec3 vViewNormal;

uniform vec4 uColor;

out vec4 fragColor;

void main() {
    // Flat handle colors, darkened where the surface turns away from the viewer.
    float facing = abs(normalize(vViewNormal).z);
    fragColor = vec4(uColor.rgb * (0.6 + 0.4 * facing), uColor.a);
}
