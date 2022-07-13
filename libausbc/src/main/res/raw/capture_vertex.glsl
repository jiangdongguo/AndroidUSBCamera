uniform mat4 uMVPMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoordinate;
varying vec2 vTextureCoord;
void main()
{
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = aTextureCoordinate.xy;
}