#version 330

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 localUv;
flat in vec2 windowSize;
flat in vec2 sourceOrigin;
flat in vec3 packedPortal;

out vec4 fragColor;

const bool uIsFullscreen = false;
const float uDuration = 1.5;
const float uScale = 1.0;
const float uTurbulence = 1.0;
bool uForOpening = packedPortal.x < 0.0;
float uProgress = clamp(abs(packedPortal.x), 0.0, 1.0);
vec2 uSeed = clamp(packedPortal.yz, 0.0, 1.0);
vec3 uColor = vertexColor.rgb;
vec2 uSize = windowSize;
float uPadding = 0.0;
#define iTexCoord localUv

vec4 getInputColor(vec2 coords) {
    if (coords.x < 0.0 || coords.x > 1.0 || coords.y < 0.0 || coords.y > 1.0) {
        return vec4(0.0);
    }

    vec2 textureSizePixels = vec2(textureSize(Sampler0, 0));
    vec2 sampleUv = vec2(
        (sourceOrigin.x + coords.x * uSize.x) / textureSizePixels.x,
        1.0 - ((sourceOrigin.y + coords.y * uSize.y) / textureSizePixels.y)
    );

    vec4 color = texture(Sampler0, sampleUv);
    if (color.a > 0.0) {
        color.rgb /= color.a;
    }

    return color;
}

vec4 alphaOver(vec4 under, vec4 over) {
    if (under.a == 0.0 && over.a == 0.0) {
        return vec4(0.0);
    }

    float alpha = mix(under.a, 1.0, over.a);
    return vec4(mix(under.rgb * under.a, over.rgb, over.a) / alpha, alpha);
}

vec3 tritone(float val, vec3 shadows, vec3 midtones, vec3 highlights) {
    if (val < 0.5) {
        return mix(shadows, midtones, smoothstep(0.0, 1.0, val * 2.0));
    }
    return mix(midtones, highlights, smoothstep(0.0, 1.0, val * 2.0 - 1.0));
}

vec3 darken(vec3 color, float fac) {
    return color * (1.0 - fac);
}

float getEdgeMask(vec2 uv, vec2 maxUv, float fadeWidth) {
    float mask = 1.0;
    if (!uIsFullscreen) {
        mask *= smoothstep(0.0, 1.0, clamp(uv.x / fadeWidth, 0.0, 1.0));
        mask *= smoothstep(0.0, 1.0, clamp(uv.y / fadeWidth, 0.0, 1.0));
        mask *= smoothstep(0.0, 1.0, clamp((maxUv.x - uv.x) / fadeWidth, 0.0, 1.0));
        mask *= smoothstep(0.0, 1.0, clamp((maxUv.y - uv.y) / fadeWidth, 0.0, 1.0));
    }
    return mask;
}

float getAbsoluteEdgeMask(float fadePixels, float offset) {
    float padding = max(0.0, uPadding - fadePixels * offset);
    vec2 uv = iTexCoord.st * uSize - padding;
    return getEdgeMask(uv, uSize - 2.0 * padding, fadePixels);
}

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float simplex2D(vec2 p) {
    const float K1 = 0.366025404;
    const float K2 = 0.211324865;

    vec2 i = floor(p + (p.x + p.y) * K1);
    vec2 a = p - i + (i.x + i.y) * K2;
    float m = step(a.y, a.x);
    vec2 o = vec2(m, 1.0 - m);
    vec2 b = a - o + K2;
    vec2 c = a - 1.0 + 2.0 * K2;
    vec3 h = max(0.5 - vec3(dot(a, a), dot(b, b), dot(c, c)), 0.0);
    vec3 n = h * h * h * h * vec3(
        dot(a, -1.0 + 2.0 * hash22(i + 0.0)),
        dot(b, -1.0 + 2.0 * hash22(i + o)),
        dot(c, -1.0 + 2.0 * hash22(i + 1.0))
    );
    return 0.5 + 0.5 * dot(n, vec3(70.0));
}

float simplex2DFractal(vec2 p) {
    mat2 m = mat2(1.6, 1.2, -1.2, 1.6);
    float f = 0.5000 * simplex2D(p);
    p = m * p;
    f += 0.2500 * simplex2D(p);
    p = m * p;
    f += 0.1250 * simplex2D(p);
    p = m * p;
    f += 0.0625 * simplex2D(p);
    return f;
}

vec4 getFireColor(float val) {
    return vec4(tritone(val, vec3(0.0), uColor, vec3(1.0)), val);
}

vec2 getStartPos() {
    vec2 corner = step(vec2(0.5), uSeed);
    return mix(vec2(0.12), vec2(0.88), corner);
}

void main() {
    float scorchWidth = 0.2 * uScale;
    float burnWidth = 0.03 * uScale;
    float smokeWidth = 0.9 * uScale;
    float flameWidth = 0.2 * uScale;

    float hideThreshold = mix(uForOpening ? 0.0 : -scorchWidth, 1.0 + smokeWidth, uProgress);

    vec2 scorchRange = uForOpening ? vec2(hideThreshold - scorchWidth, hideThreshold)
            : vec2(hideThreshold, hideThreshold + scorchWidth);
    vec2 burnRange = vec2(hideThreshold - burnWidth, hideThreshold + burnWidth);
    vec2 flameRange = vec2(hideThreshold - flameWidth, hideThreshold);
    vec2 smokeRange = vec2(hideThreshold - smokeWidth, hideThreshold);

    float circle = length((iTexCoord - getStartPos()) * (uSize.xy / max(uSize.x, uSize.y)));

    vec2 uv = iTexCoord / uScale * uSize / 1.5;
    float smokeNoise = simplex2DFractal(uv * 0.01 + uSeed + uProgress * vec2(0.0, 0.3 * uDuration));
    float gradient = mix(circle, smokeNoise, 200.0 * uTurbulence * uScale / max(uSize.x, uSize.y));

    float smokeMask = smoothstep(0.0, 1.0, (gradient - smokeRange.x) / smokeWidth) * getAbsoluteEdgeMask(100.0, 0.3);
    float flameMask = smoothstep(0.0, 1.0, (gradient - flameRange.x) / flameWidth) * getAbsoluteEdgeMask(20.0, 0.0);
    float fireMask = smoothstep(1.0, 0.0, abs(gradient - hideThreshold) / burnWidth);
    float scorchMask = smoothstep(1.0, 0.0, (gradient - scorchRange.x) / scorchWidth);

    if (uForOpening) {
        scorchMask = 1.0 - scorchMask;
    }

    vec4 outColor = vec4(0.0);

    if ((!uForOpening && gradient > hideThreshold) || (uForOpening && gradient < hideThreshold)) {
        vec2 distort = vec2(0.0);
#ifndef GL_ES
        if (scorchRange.x < gradient && gradient < scorchRange.y) {
            distort = vec2(dFdx(gradient), dFdy(gradient)) * scorchMask * 5.0;
        }
#endif
        outColor = getInputColor(iTexCoord + distort);
    }

    if (smokeRange.x < gradient && gradient < smokeRange.y) {
        float smoke = smokeMask * smokeNoise;
        outColor = alphaOver(outColor, vec4(0.5 * vec3(smoke), smoke));

        float emberNoise = simplex2DFractal(
                uv * 0.05 + uSeed - smokeNoise * vec2(0.0, 0.3 * smokeMask * uDuration)
        );
        float embers = clamp(pow(emberNoise + 0.3, 100.0), 0.0, 2.0) * smoke;
        outColor += getFireColor(embers);
    }

    if (scorchRange.x < gradient && gradient < scorchRange.y) {
        outColor.rgb = mix(outColor.rgb, mix(outColor.rgb, vec3(0.1, 0.05, 0.02), 0.4), scorchMask);
    }

    if (min(burnRange.x, flameRange.x) < gradient && gradient < max(burnRange.y, flameRange.y)) {
        float flameNoise = simplex2DFractal(
                uv * 0.02 + uSeed + smokeNoise * vec2(0.0, 1.0 * uDuration) + vec2(0.0, uProgress * uDuration)
        );

        if (flameRange.x < gradient && gradient < flameRange.y) {
            float flame = clamp(pow(flameNoise + 0.3, 20.0), 0.0, 2.0) * flameMask;
            flame += clamp(pow(flameNoise + 0.4, 10.0), 0.0, 2.0) * flameMask * flameMask * 0.1;
            outColor += getFireColor(flame);
        }

        if (burnRange.x < gradient && gradient < burnRange.y) {
            float fire = fireMask * pow(flameNoise + 0.4, 4.0) * outColor.a;
            outColor += getFireColor(fire);
        }
    }

    fragColor = vec4(outColor.rgb, outColor.a * vertexColor.a);
}
