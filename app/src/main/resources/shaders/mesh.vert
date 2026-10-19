#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;

uniform mat4 uModel;
uniform mat4 uNormalMatrix;
uniform mat4 uViewProj;

out vec3 vNormal;
out vec2 vUv;
out vec3 vGridPos;
out vec3 vLocalNormal;

void main() {
    vec4 world = uModel * vec4(aPosition, 1.0);
    vNormal = mat3(uNormalMatrix) * aNormal;
    vUv = aUv;

    // Grid space: world units, but with the object's rotation removed, so the
    // grid follows rotated objects. The model columns are the rotated axes, each
    // scaled by size; normalizing them recovers the rotation.
    mat3 rotation = mat3(normalize(uModel[0].xyz), normalize(uModel[1].xyz), normalize(uModel[2].xyz));
    vGridPos = transpose(rotation) * world.xyz;
    vLocalNormal = aNormal;
    gl_Position = uViewProj * world;
}
