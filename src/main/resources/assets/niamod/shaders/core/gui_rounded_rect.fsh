#version 330

in vec4 vertexColor;
in vec2 localPos;
flat in vec2 rectSize;
flat in ivec2 packedData;

out vec4 fragColor;

float roundedBoxSdf(vec2 point, vec2 halfSize, float radius) {
    vec2 delta = abs(point) - halfSize + radius;
    return length(max(delta, 0.0)) + min(max(delta.x, delta.y), 0.0) - radius;
}

void main() {
    float radius = min(float(packedData.x), min(rectSize.x, rectSize.y) * 0.5);
    vec2 point = localPos - rectSize * 0.5;
    float distanceToEdge = roundedBoxSdf(point, rectSize * 0.5, radius);
    float aa = max(fwidth(distanceToEdge) * 1.5, 0.75);
    float alpha = 1.0 - smoothstep(0.0, aa, distanceToEdge);
    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
