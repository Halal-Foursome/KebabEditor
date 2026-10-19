#version 330 core

in vec3 vWorldPos;

uniform vec4 uColor;
uniform vec3 uCameraPos;
uniform float uFadeDistance;

out vec4 fragColor;

void main() {
    float distance = length(vWorldPos.xz - uCameraPos.xz);
    float fade = 1.0 - smoothstep(uFadeDistance * 0.4, uFadeDistance, distance);
    fragColor = vec4(uColor.rgb, uColor.a * fade);
}
