attribute vec4 aPosition;
attribute vec4 aTextureCoordinate;
varying vec2 vTextureCoord;
uniform float timeStamps;
const float PI = 3.1415926;

void main()
{
    float duration = 0.9; // zoom period, 0.9s
    float maxAmplitude = 0.3; // scale peak offset
    float modTime = mod(timeStamps, duration);
    // zoom range [1.0f, 1.3f]
    float amplitude = 1.0 + maxAmplitude * abs(sin((modTime / duration) * PI));

    gl_Position = vec4(aPosition.x * amplitude, aPosition.y * amplitude, aPosition.zw);
    vTextureCoord = aTextureCoordinate.xy;
}