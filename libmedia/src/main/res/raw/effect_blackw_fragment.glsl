precision mediump float;
uniform sampler2D uTextureSampler;
varying vec2 vTextureCoord;
void main()
{
    vec4 tempColor = texture2D(uTextureSampler, vTextureCoord);
    // Get the grayscale value of each pixel
    float luminance = tempColor.r * 0.299 + tempColor.g * 0.584 + tempColor.b * 0.114;
    gl_FragColor = vec4(vec3(luminance), tempColor.a);
}