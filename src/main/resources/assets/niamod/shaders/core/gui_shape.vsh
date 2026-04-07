#version 330

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

out vec4 vertexColor;
out vec2 localPos;
flat out vec2 rectSize;
flat out ivec2 packedData;

void main() {
    gl_Position = vec4(Position, 1.0);
    vertexColor = Color;
    localPos = UV0;
    rectSize = vec2(UV1);
    packedData = UV2;
}
