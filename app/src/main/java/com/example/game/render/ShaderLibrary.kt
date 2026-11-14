package com.example.game.render

import android.opengl.GLES30
import android.util.Log

class ShaderProgram(vertexSrc: String, fragmentSrc: String) {
    val programId: Int

    init {
        val vs = compile(GLES30.GL_VERTEX_SHADER, vertexSrc)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vs)
        GLES30.glAttachShader(programId, fs)
        GLES30.glLinkProgram(programId)
        val status = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("AetherfallShader", "Link error: ${GLES30.glGetProgramInfoLog(programId)}")
        }
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
    }

    private fun compile(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("AetherfallShader", "Compile error: ${GLES30.glGetShaderInfoLog(shader)}")
        }
        return shader
    }

    fun use() = GLES30.glUseProgram(programId)
    fun uniform(name: String): Int = GLES30.glGetUniformLocation(programId, name)
    fun destroy() = GLES30.glDeleteProgram(programId)
}

object ShaderLibrary {

    const val WORLD_VERTEX_SHADER = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;
layout(location = 3) in vec2 aUV;

uniform mat4 uMVP;
uniform mat4 uModel;
uniform mat3 uNormalMat;

out vec3 vWorldPos;
out vec3 vNormal;
out vec3 vColor;
out vec2 vUV;

void main() {
    vec4 wp = uModel * vec4(aPos, 1.0);
    vWorldPos = wp.xyz;
    vNormal = normalize(uNormalMat * aNormal);
    vColor = aColor;
    vUV = aUV;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

    const val WORLD_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
in vec3 vWorldPos;
in vec3 vNormal;
in vec3 vColor;
in vec2 vUV;

uniform vec3 uCameraPos;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uSkyAmbient;
uniform vec3 uGroundAmbient;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform float uRoughness;
uniform float uWetness;
uniform vec3 uEmissive;
uniform vec3 uTint;
uniform vec3 uPointLightPos;
uniform vec3 uPointLightColor;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    // Procedural micro-detail & normal perturbation for crisp HD look on Mali GPUs
    float grain = hash21(floor(vWorldPos.xz * 7.5 + vUV * 12.0));
    float microDetail = mix(0.91, 1.08, grain);

    vec3 N = normalize(vNormal + vec3((grain - 0.5) * 0.08, 0.0, (grain - 0.5) * 0.08));
    vec3 L = normalize(uSunDir);
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 H = normalize(L + V);

    float ndl = max(dot(N, L), 0.0);
    // Hemisphere ambient lighting
    float hemi = 0.5 * N.y + 0.5;
    vec3 ambient = mix(uGroundAmbient, uSkyAmbient, hemi);

    // Specular with rain wetness boost
    float effRoughness = mix(uRoughness, 0.18, uWetness);
    float shininess = mix(96.0, 8.0, effRoughness);
    float spec = pow(max(dot(N, H), 0.0), shininess) * mix(0.65, 0.08, effRoughness);

    // Dynamic point light (forge, volcano glow, or dungeon torch)
    vec3 pVec = uPointLightPos - vWorldPos;
    float pDist = length(pVec);
    float pAtten = 1.0 / (1.0 + 0.025 * pDist * pDist);
    float pNdl = max(dot(N, normalize(pVec)), 0.0);
    vec3 pointContrib = uPointLightColor * pNdl * pAtten;

    vec3 baseCol = vColor * uTint * microDetail;
    vec3 lit = baseCol * (ambient + uSunColor * ndl * 1.15 + pointContrib)
             + uSunColor * spec * (0.4 + 0.6 * uWetness)
             + uEmissive;

    // Exponential atmospheric fog
    float dist = length(uCameraPos - vWorldPos);
    float fogFactor = 1.0 - exp(-pow(dist * uFogDensity, 1.65));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec3 finalCol = mix(lit, uFogColor, fogFactor);
    fragColor = vec4(finalCol, 1.0);
}
"""

    const val WATER_VERTEX_SHADER = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;
layout(location = 3) in vec2 aUV;

uniform mat4 uMVP;
uniform mat4 uModel;
uniform float uTime;

out vec3 vWorldPos;
out vec2 vUV;

void main() {
    vec4 wp = uModel * vec4(aPos, 1.0);
    float wave = sin(wp.x * 0.45 + uTime * 2.1) * 0.11 + cos(wp.z * 0.52 - uTime * 1.7) * 0.09;
    wp.y += wave;
    vWorldPos = wp.xyz;
    vUV = aUV;
    gl_Position = uMVP * vec4(aPos.x, aPos.y + wave, aPos.z, 1.0);
}
"""

    const val WATER_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
in vec3 vWorldPos;
in vec2 vUV;

uniform vec3 uCameraPos;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform float uTime;

out vec4 fragColor;

void main() {
    float wx = cos(vWorldPos.x * 0.8 + uTime * 2.4) * 0.14 + sin(vWorldPos.z * 1.3 - uTime * 1.9) * 0.10;
    float wz = sin(vWorldPos.z * 0.9 + uTime * 2.1) * 0.14 + cos(vWorldPos.x * 1.2 + uTime * 1.5) * 0.10;
    vec3 N = normalize(vec3(wx, 1.0, wz));
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 L = normalize(uSunDir);
    vec3 H = normalize(L + V);

    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.0);
    float spec = pow(max(dot(N, H), 0.0), 72.0);
    vec3 deepWater = vec3(0.04, 0.22, 0.34);
    vec3 shallowTint = vec3(0.10, 0.48, 0.58);
    vec3 col = mix(deepWater, shallowTint, 0.35 + 0.25 * sin(vWorldPos.x * 0.2 + uTime));
    col = mix(col, uFogColor * 1.15, fresnel * 0.65) + uSunColor * spec * 0.95;

    float dist = length(uCameraPos - vWorldPos);
    float fogFactor = clamp(1.0 - exp(-pow(dist * uFogDensity, 1.65)), 0.0, 1.0);
    fragColor = vec4(mix(col, uFogColor, fogFactor), 0.84);
}
"""

    const val SKY_VERTEX_SHADER = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
uniform mat4 uMVP;
out vec3 vDir;
void main() {
    vDir = normalize(aPos);
    gl_Position = (uMVP * vec4(aPos, 1.0)).xyww;
}
"""

    const val SKY_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
in vec3 vDir;

uniform vec3 uZenithColor;
uniform vec3 uHorizonColor;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform float uStarAlpha;

out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

void main() {
    vec3 dir = normalize(vDir);
    float h = clamp(dir.y, 0.0, 1.0);
    vec3 sky = mix(uHorizonColor, uZenithColor, pow(h, 0.58));

    // Sun / Moon glow disc
    float sunDot = max(dot(dir, normalize(uSunDir)), 0.0);
    sky += uSunColor * pow(sunDot, 22.0) * 0.45;
    sky += uSunColor * smoothstep(0.9965, 0.9992, sunDot) * 1.4;

    // Night starfield
    if (uStarAlpha > 0.01 && dir.y > 0.05) {
        vec3 cell = floor(dir * 190.0);
        float s = hash31(cell);
        if (s > 0.9945) {
            sky += vec3(0.85, 0.92, 1.0) * uStarAlpha * smoothstep(0.05, 0.3, dir.y);
        }
    }

    fragColor = vec4(sky, 1.0);
}
"""

    const val VFX_VERTEX_SHADER = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;
layout(location = 3) in vec2 aUV;

uniform mat4 uMVP;
uniform mat4 uModel;

out vec3 vColor;

void main() {
    vColor = aColor;
    gl_Position = uMVP * (uModel * vec4(aPos, 1.0));
}
"""

    const val VFX_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
in vec3 vColor;
uniform vec4 uColorAlpha;
out vec4 fragColor;
void main() {
    fragColor = vec4(vColor * uColorAlpha.rgb, uColorAlpha.a);
}
"""
}
