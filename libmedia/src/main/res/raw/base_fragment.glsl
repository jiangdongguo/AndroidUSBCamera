precision highp float;
uniform sampler2D uTextureSampler;
varying vec2 vTextureCoord;
void main()
{
    gl_FragColor = texture2D(uTextureSampler, vTextureCoord);
}
