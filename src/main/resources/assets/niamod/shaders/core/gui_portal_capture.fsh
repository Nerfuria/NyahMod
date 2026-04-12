#version 330

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 localUv;
flat in vec2 windowSize;
flat in vec2 sourceOrigin;
flat in vec3 packedPortal;

out vec4 fragColor;

const bool uIsFullscreen = false;
const float uDuration = 1.25;
const float uPadding = 0.0;
const float uRotationSpeed = 0.3;
const float uWhirling = 1.0;
const float uDetails = 15.0;

const float PORTAL_WOBBLE_TIME = 0.8;
const float PORTAL_WOBBLE_STRENGTH = 1.2;
const float GLOW_EDGE_WIDTH = 5.0;
const float WINDOW_SCALE = 0.3;
const float WINDOW_SQUISH = 1.0;
const float WINDOW_TILT = -1.0;
const float PORTAL_OPEN_TIME = 0.4;
const float PORTAL_CLOSE_TIME = 0.4;
const float WINDOW_OPEN_TIME = 0.35;

bool uForOpening = packedPortal.x < 0.0;
float uProgress = clamp(abs(packedPortal.x), 0.0, 1.0);
vec2 uSeed = clamp(packedPortal.yz, 0.0, 1.0);
vec3 uColor = vertexColor.rgb;
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

vec3 darken(vec3 color, float fac) {
    return color * (1.0 - fac);
}

vec3 lighten(vec3 color, float fac) {
    return color + (vec3(1.0) - color) * fac;
}

float easeInBack(float x, float e) {
    return x * x * ((e + 1.0) * x - e);
}

float easeOutBack(float x, float e) {
    float p = x - 1.0;
    return p * p * ((e + 1.0) * p + e) + 1.0;
}

vec2 whirl(vec2 coords, float warping, float rotation) {
    float angle = pow(1.0 - length(coords), 2.0) * warping + rotation;
    float s = sin(angle);
    float c = cos(angle);
    return vec2(dot(coords, vec2(c, -s)), dot(coords, vec2(s, c)));
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

vec2 getPortalWobble(vec2 coords) {
    float progress = (uForOpening ? (1.0 - uProgress) : uProgress) / WINDOW_OPEN_TIME * uDuration;
    progress = clamp(1.0 - abs((progress - 1.0) / PORTAL_WOBBLE_TIME), 0.0, 1.0);
    progress = easeInBack(progress, 1.7);
    float dist = length(coords);

    return coords * (1.0 - dist) * exp(-dist) * progress * PORTAL_WOBBLE_STRENGTH;
}

float getPortalScale() {
    float scale = 1.0;
    float closeTime = PORTAL_CLOSE_TIME / uDuration;
    float openTime = PORTAL_OPEN_TIME / uDuration;
    if (uProgress < openTime) {
        scale = easeOutBack(uProgress / openTime, 1.5);
    } else if (uProgress > 1.0 - closeTime) {
        scale = easeOutBack(1.0 - (uProgress - 1.0 + closeTime) / closeTime, 1.5);
    }

    return scale;
}

vec2 getRandomDisplace(vec2 seed, float scale) {
    return vec2(simplex2D(seed * scale + uSeed) - 0.5,
            simplex2D(seed * scale + uSeed + vec2(7.89, 123.0)) - 0.5);
}

vec2 getWhirledCoords(vec2 coords, float speedMultiplier, float warpMultiplier) {
    float rotation = uRotationSpeed * uProgress * uDuration * speedMultiplier;
    float warping = uWhirling * (6.0 + 1.5 * uProgress) * warpMultiplier;
    return whirl(coords, warping, rotation);
}

float roundedBoxSdf(vec2 point, vec2 halfSize, float radius) {
    vec2 delta = abs(point) - halfSize + radius;
    return length(max(delta, 0.0)) + min(max(delta.x, delta.y), 0.0) - radius;
}

vec4 getPortalColor() {
    vec2 coords = (iTexCoord.st - vec2(0.5)) * 2.0;
    coords *= 1.5;

    float scale = getPortalScale();
    coords /= max(scale * 0.5 + 0.5, 0.01);

    vec2 wobble = getPortalWobble(coords);
    float detailScale = 10000.0 / (uSize.x + uSize.y) / uDetails;

    vec2 layerCoords = getWhirledCoords(coords - wobble * 1.0, 0.25, 1.0);
    vec2 displace = getRandomDisplace(layerCoords, 2.1);
    layerCoords += displace * 0.1;
    float dist = length(layerCoords);
    float alpha = dist > 1.0 ? 0.0 : 1.0;
    vec4 color = vec4(mix(darken(uColor, 0.8), darken(uColor, 0.2), pow(dist, 5.0)), alpha);
    float rand = dot(displace, displace);

    float noise = simplex2D(layerCoords / detailScale * 1.0 + vec2(12.3, 56.4) + uSeed);
    vec4 layer = vec4(darken(uColor, 0.3), noise > 0.6 ? alpha : 0.0);
    color = alphaOver(color, layer);

    layerCoords = getWhirledCoords(coords - wobble * 1.5, 0.75, 0.5);
    displace = getRandomDisplace(layerCoords, 12.2);
    layerCoords += displace * 0.1;
    noise = simplex2D(layerCoords / detailScale * 1.3 + uSeed);
    layer = vec4(uColor, noise > 0.6 ? alpha : 0.0);
    color = alphaOver(color, layer);
    color = clamp(color, 0.0, 1.0);

    float edge = mix(1.0, 5.0, rand) * GLOW_EDGE_WIDTH * detailScale - 150.0 * abs(dist - 1.0);
    layer = vec4(uColor, clamp(edge, 0.0, 1.0));
    color = alphaOver(color, layer);

    layerCoords = getWhirledCoords(coords - wobble * 1.8, 1.25, 0.0);
    noise = simplex2D(layerCoords / detailScale * 3.0 + uSeed);
    layer.rgb = lighten(uColor, 0.8) * clamp(pow(noise * rand + 0.9, 50.0), 0.0, 1.0);
    color.rgb += layer.rgb;

    color.a *= pow(clamp(scale, 0.0, 1.0), 2.0);
    return clamp(color, 0.0, 1.0);
}

vec4 getWindowColor() {
    float progress = (uForOpening ? (1.0 - uProgress) : uProgress) / WINDOW_OPEN_TIME * uDuration;
    progress = easeInBack(clamp(progress, 0.0, 1.0), 1.2);

    vec2 coords = iTexCoord.st * 2.0 - 1.0;
    coords /= mix(1.0, WINDOW_SCALE, progress);
    coords.y /= mix(1.0, (1.0 - 0.2 * WINDOW_SQUISH), progress);
    coords.x /= mix(1.0, 1.0 - 0.1 * WINDOW_TILT * coords.y, progress);
    coords = coords * 0.5 + 0.5;

    vec4 oColor = getInputColor(coords);
    vec2 localPoint = coords * uSize - uSize * 0.5;
    float panelScale = min(uSize.x / 400.0, uSize.y / 300.0);
    float cornerRadius = panelScale * 12.0;
    float maskInset = panelScale * 4.0;
    vec2 maskHalfSize = max(uSize * 0.5 - vec2(maskInset), vec2(1.0));
    float maskRadius = max(cornerRadius - maskInset, 0.0);
    float roundedDistance = roundedBoxSdf(localPoint, maskHalfSize, maskRadius);
    float roundedAa = max(fwidth(roundedDistance) * 1.5, 0.75);
    float roundedMask = 1.0 - smoothstep(0.0, roundedAa, roundedDistance);
    oColor.a *= roundedMask;
    oColor.a *= clamp((1.0 - progress) * 3.0, 0.0, 1.0);
    return oColor;
}

void main() {
    vec4 portal = getPortalColor();
    vec4 window = getWindowColor();
    vec4 outColor = alphaOver(portal, window);
    fragColor = vec4(outColor.rgb, outColor.a * vertexColor.a);
}
