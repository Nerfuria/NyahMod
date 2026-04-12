#version 330

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 vertexColor;
out vec2 localUv;
flat out vec2 windowSize;
flat out vec2 sourceOrigin;
flat out vec3 packedPortal;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vertexColor = Color;
    localUv = UV0;
    windowSize = vec2(UV1);
    sourceOrigin = vec2(UV2);
    packedPortal = Normal;
}
