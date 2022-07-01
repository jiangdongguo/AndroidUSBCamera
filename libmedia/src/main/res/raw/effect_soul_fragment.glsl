precision mediump float;

uniform sampler2D uTextureSampler;
varying vec2 vTextureCoord;
//timestamp
uniform float timeStamps;

void main() {
    //The duration of an out-of-body effect
    float duration = 0.7;
    // upper limit of transparency
    float maxAlpha = 0.4;
    //The upper limit of image enlargement
    float maxScale = 1.8;

    //The current progress (timestamp and duration are modulo modulo), then divide by the duration to get [0, 1], which is the percentage
    float progress = mod(timeStamps, duration) / duration; // 0~1
    //Current transparency [0.4, 0]
    float alpha = maxAlpha * (1.0 - progress);
    //Current zoom ratio [1.0, 1.8]
    float scale = 1.0 + (maxScale - 1.0) * progress;

    //Get the enlarged texture coordinates
    //Reduce the distance from the x/y value of the texture coordinate corresponding to the vertex coordinate to the center point, reduce a certain ratio, just change the texture coordinate, and keep the vertex coordinate unchanged, so as to achieve the stretching effect
    float weakX = 0.5 + (vTextureCoord.x - 0.5) / scale;
    float weakY = 0.5 + (vTextureCoord.y - 0.5) / scale;
    vec2 weakTextureCoords = vec2(weakX, weakY);

    //Get the texture coordinates of the current pixel, the enlarged texture coordinates
    vec4 weakMask = texture2D(uTextureSampler, weakTextureCoords);

    vec4 mask = texture2D(uTextureSampler, vTextureCoord);
    //2, color mixing built-in function mix / mixing equation
    gl_FragColor = mask * (1.0 - alpha) + weakMask * alpha;
}