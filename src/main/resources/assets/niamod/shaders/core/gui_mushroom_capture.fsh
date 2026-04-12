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
const float uScaleStyle = 1.0;
const float uSparkCount = 4.0;
const vec4 uSparkColor = vec4(1.0, 1.0, 1.0, 1.0);
const float uSparkRotation = 0.3;
const vec4 uRaysColor = vec4(1.0, 1.0, 1.0, 1.0);
const float uRingCount = 3.0;
const float uRingRotation = 1.33;
const float uStarCount = 5.0;
const vec4 uStarColor0 = vec4(233.0 / 255.0, 249.0 / 255.0, 1.0 / 255.0, 1.0);
const vec4 uStarColor1 = vec4(233.0 / 255.0, 249.0 / 255.0, 1.0 / 255.0, 1.0);
const vec4 uStarColor2 = vec4(91.0 / 255.0, 1.0, 1.0 / 255.0, 1.0);
const vec4 uStarColor3 = vec4(91.0 / 255.0, 1.0, 1.0 / 255.0, 1.0);
const vec4 uStarColor4 = vec4(0.0, 240.0 / 255.0, 236.0 / 255.0, 1.0);
const vec4 uStarColor5 = vec4(0.0, 240.0 / 255.0, 236.0 / 255.0, 1.0);

bool uForOpening = packedPortal.x < 0.0;
float uProgress = clamp(abs(packedPortal.x), 0.0, 1.0);
vec2 uSeed = clamp(packedPortal.yz, 0.0, 1.0);
vec2 uSize = windowSize;
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

float remap(float value, float inMin, float inMax, float outMin, float outMax) {
    float t = clamp((value - inMin) / (inMax - inMin), 0.0, 1.0);
    return mix(outMin, outMax, t);
}

float easeInExpo(float x) {
    return x <= 0.0 ? 0.0 : pow(2.0, 10.0 * x - 10.0);
}

float easeInSine(float x) {
    return 1.0 - cos((x * 3.14159265) * 0.5);
}

float easeInOutSine(float x) {
    return -(cos(3.14159265 * x) - 1.0) * 0.5;
}

float easeInQuad(float x) {
    return x * x;
}

vec2 rotate(vec2 uv, float angle, vec2 pivot) {
    float s = sin(angle);
    float c = cos(angle);
    uv -= pivot;
    uv = mat2(c, -s, s, c) * uv;
    return uv + pivot;
}

vec3 getPosByAngle(float angle) {
    return vec3(cos(angle), sin(angle), 0.0);
}

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

vec2 hash21(float p) {
    vec3 p3 = fract(vec3(p, p + 1.0, p + 2.0) * vec3(0.1031, 0.1030, 0.0973));
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

vec2 scaleUV(vec2 uv, vec2 scale) {
    uv = uv * 2.0 - 1.0;
    uv /= max(mix(vec2(1.0), vec2(0.0), scale), vec2(0.001));
    uv = uv * 0.5 + 0.5;
    return uv;
}

float getStar(vec2 uv, vec2 center, float npoints, float radiusRatio, float size, float rotation) {
    float radiusMax = 1.0;
    float radiusMin = radiusMax * radiusRatio;
    float starangle = 2.0 * 3.14159265 / npoints;
    rotation += 3.14159265 * 0.5 - starangle;

    vec3 p0 = (radiusMax * size) * getPosByAngle(rotation);
    vec3 p1 = (radiusMin * size) * getPosByAngle(starangle + rotation);

    vec2 curPosuv = uv - center;
    float curRadius = length(curPosuv);
    float curPosAngle = atan(curPosuv.y, curPosuv.x) - rotation;

    float a = fract(curPosAngle / starangle);
    if (a >= 0.5) {
        a = 1.0 - a;
    }

    a = a * starangle;
    vec3 curPos = curRadius * getPosByAngle(a + rotation);

    vec3 dir0 = p1 - p0;
    vec3 dir1 = curPos - p0;
    return step(0.0, cross(dir0, dir1).z);
}

float getSpark(vec2 uv, vec2 center, float brightness, float size, float rotation) {
    brightness = clamp(brightness, 0.001, 1.0);
    size = clamp(size, 0.001, 1.0);
    float bn = mix(0.0, 0.07, brightness);

    uv = uv + vec2(0.5);
    uv = uv - center;
    uv = scaleUV(uv, vec2(1.0 - size));
    uv = rotate(uv, rotation, vec2(0.5));

    float p = mix(-1.0, 1000.0, easeInExpo(bn));
    float m = mix(
        0.0,
        1.0,
        clamp(pow(abs(uv.x - 0.5) * 2.0, p) + pow(abs(uv.y - 0.5) * 2.0, p), 0.0, 1.0)
    );

    float mask = easeInSine(1.0 - (m - bn)) - 0.004;
    return clamp(mask, 0.0, 1.0);
}

float zeroStartEnd(float t, float maxSize, float power) {
    float s = -pow((t - 0.5) / 0.5, power) + 1.0;
    return clamp(s, 0.0, 1.0) * maxSize;
}

float eightBitScale(float progress) {
    float scale = 1.0;
    if (progress <= 0.1) {
        scale = 0.25;
    } else if (progress <= 0.2) {
        scale = 0.5;
    } else if (progress <= 0.3) {
        scale = 0.25;
    } else if (progress <= 0.4) {
        scale = 0.5;
    } else if (progress <= 0.5) {
        scale = 0.25;
    } else if (progress <= 0.6) {
        scale = 0.5;
    } else if (progress <= 0.7) {
        scale = 1.0;
    } else if (progress <= 0.8) {
        scale = 0.25;
    } else if (progress <= 0.9) {
        scale = 0.5;
    }
    return scale;
}

vec4 getStarColor(float v, float alpha) {
    v = clamp(v, 0.0, 1.0);
    vec4 c0 = vec4(uStarColor0.rgb, alpha * uStarColor0.a);
    vec4 c1 = vec4(uStarColor1.rgb, alpha * uStarColor1.a);
    vec4 c2 = vec4(uStarColor2.rgb, alpha * uStarColor2.a);
    vec4 c3 = vec4(uStarColor3.rgb, alpha * uStarColor3.a);
    vec4 c4 = vec4(uStarColor4.rgb, alpha * uStarColor4.a);
    vec4 c5 = vec4(uStarColor5.rgb, alpha * uStarColor5.a);

    if (v <= 0.1666) return mix(c0, c1, v / 0.1666);
    if (v <= 0.3332) return mix(c1, c2, (v - 0.1666) / (0.3332 - 0.1666));
    if (v <= 0.4998) return mix(c2, c3, (v - 0.3332) / (0.4998 - 0.3332));
    if (v <= 0.6664) return mix(c3, c4, (v - 0.4998) / (0.6664 - 0.4998));
    if (v <= 0.8330) return mix(c4, c5, (v - 0.6664) / (0.8330 - 0.6664));
    return c5;
}

vec4 getSparks(float progress) {
    float aspect = uSize.x / max(uSize.y, 1.0);
    vec2 uv = iTexCoord.st * vec2(aspect, 1.0);
    vec4 result = vec4(0.0);

    float xEdge = -pow((uv.x - aspect * 0.5) / max(aspect * 0.5, 0.001), 8.0) + 1.0;
    xEdge = clamp(xEdge, 0.0, 1.0);

    for (float spark = 0.0; spark < uSparkCount; spark += 1.0) {
        vec2 h = hash21(spark + uSeed.x);
        float y = mix(0.0 - h.y, 1.0 + (1.0 - h.y), progress);
        y = clamp(y, 0.0, 1.0);

        float x = 0.66 * sin(h.x * 6.2831853);
        x += 0.5 * aspect;

        float sparkMask = getSpark(
                uv,
                vec2(x, y),
                zeroStartEnd(y, 1.0, 4.0) * xEdge,
                zeroStartEnd(y, 0.5, 4.0) * xEdge,
                progress * 6.2831853 * uSparkRotation
        );

        result = alphaOver(result, vec4(uSparkColor.rgb, uSparkColor.a * sparkMask));
    }

    return result;
}

vec4 getRays(float progress) {
    vec2 rayUv = iTexCoord.st;
    rayUv *= vec2(10.0, 0.5);
    rayUv.y += progress * -1.0;
    rayUv.x += uSeed.y;

    float ray = simplex2D(rayUv);
    ray *= zeroStartEnd(iTexCoord.t, 1.0, 8.0);
    ray *= zeroStartEnd(iTexCoord.s, 1.0, 8.0);
    ray *= zeroStartEnd(progress, 1.0, 8.0);
    ray = remap(ray * 1.10, 0.0, 1.0, -5.0, 1.0);

    float alpha = clamp(uRaysColor.a * ray, 0.0, 1.0);
    return vec4(uRaysColor.rgb, alpha);
}

vec4 getStars(vec2 starUv, float aspect, float progress, float windowAlpha) {
    vec4 result = vec4(0.0);

    for (float ring = 0.0; ring < uRingCount; ring += 1.0) {
        float spread = ring * (1.0 / max(uRingCount, 1.0));
        float y = mix(0.0 - spread, 1.0 + (1.0 - spread), 1.0 - progress);
        y = clamp(y, 0.00001, 0.99999);

        for (float star = 0.0; star < uStarCount; star += 1.0) {
            float phase = progress * uRingRotation * 6.2831853 + (star * (6.2831853 / max(uStarCount, 1.0)));
            float starMask = getStar(
                    starUv,
                    vec2(sin(phase) * aspect * 0.33, y),
                    5.0,
                    0.5,
                    zeroStartEnd(y, 0.1, 2.0),
                    0.0
            );
            starMask = clamp(starMask, 0.0, 1.0);

            float depth = cos(phase);
            vec4 starColor = getStarColor(y, starMask);
            if (depth < 0.0) {
                result = alphaOver(result, starColor);
            } else {
                result = alphaOver(starColor * (1.0 - windowAlpha), result);
            }
        }
    }

    return result;
}

void main() {
    float progress = uForOpening ? 1.0 - uProgress : uProgress;

    float scale8Bit = eightBitScale(progress);
    vec2 smoothScale = vec2(easeInOutSine(progress), easeInQuad(progress));
    vec2 finalScale = mix(vec2(scale8Bit), smoothScale, uScaleStyle);

    vec4 outColor = getInputColor(scaleUV(iTexCoord.st, finalScale));
    float windowAlpha = outColor.a;

    float aspect = uSize.x / max(uSize.y, 1.0);
    vec2 starUv = vec2(iTexCoord.s - 0.5, 1.0 - iTexCoord.t) * vec2(aspect, 1.0);

    if (uSparkCount > 0.0) {
        outColor = alphaOver(outColor, getSparks(progress));
    }

    if (uRaysColor.a > 0.0) {
        outColor = alphaOver(outColor, getRays(progress));
    }

    if (uRingCount > 0.0 && uStarCount > 0.0) {
        outColor = alphaOver(outColor, getStars(starUv, aspect, progress, windowAlpha));
    }

    fragColor = vec4(outColor.rgb, outColor.a * vertexColor.a);
}
