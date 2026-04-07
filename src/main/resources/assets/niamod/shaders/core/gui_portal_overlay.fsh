#version 330

in vec4 vertexColor;
in vec2 localPos;
flat in vec2 rectSize;
flat in ivec2 packedData;

out vec4 fragColor;

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 34.45);
    return fract(point.x * point.y);
}

mat2 rotation(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, -s, s, c);
}

void main() {
    float progress = clamp(float(packedData.x) / 1000.0, 0.0, 1.0);
    float time = float(packedData.y) / 240.0;

    vec2 uv = localPos / max(rectSize, vec2(1.0));
    vec2 centered = uv * 2.0 - 1.0;
    centered.x *= rectSize.x / max(rectSize.y, 1.0);

    float distanceToCenter = length(centered);
    vec2 warped = rotation(time * 0.1 + distanceToCenter * 5.5) * centered;
    warped += centered * (0.12 + progress * 0.34) / (distanceToCenter * 7.5 + 1.0);

    float outerRadius = mix(1.36, 0.1, progress);
    float innerRadius = outerRadius * mix(0.64, 0.18, progress);

    float ring = smoothstep(outerRadius + 0.15, outerRadius - 0.03, distanceToCenter)
            - smoothstep(innerRadius + 0.09, innerRadius - 0.02, distanceToCenter);

    float spiral = sin(atan(warped.y, warped.x) * 7.0 - distanceToCenter * 26.0 + time) * 0.5 + 0.5;
    float speckles = pow(hash21(floor(warped * (20.0 + progress * 18.0)) + packedData.yy), 8.0);
    float haze = smoothstep(outerRadius + 0.32, innerRadius - 0.06, distanceToCenter);

    vec3 portalColor = mix(vertexColor.rgb * 0.22, vertexColor.rgb, spiral);
    portalColor += vertexColor.rgb * speckles * 1.35;
    portalColor += vec3(0.18, 0.42, 0.12) * haze * 0.55;

    float alpha = ring * 0.92 + haze * 0.18 + speckles * 0.45;
    alpha *= smoothstep(0.0, 0.12, progress);
    alpha = clamp(alpha, 0.0, 1.0) * vertexColor.a;
    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(portalColor, alpha);
}
