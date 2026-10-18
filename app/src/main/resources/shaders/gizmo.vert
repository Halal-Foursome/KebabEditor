#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uViewProj;

out vec3 vViewNormal;

void main() {
    // Gizmo transforms are rotation plus uniform scale, so no inverse-transpose is needed.
    vViewNormal = mat3(uView) * mat3(uModel) * aNormal;
    gl_Position = uViewProj * uModel * vec4(aPosition, 1.0);
}
