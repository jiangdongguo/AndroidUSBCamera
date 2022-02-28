uniform mat4 uMVPMatrix;
attribute vec4 aPosition;
uniform mat4 uStMatrix;
attribute vec4 aTextureCoordinate;
varying vec2 vTextureCoord;
void main()
{
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = (uStMatrix * aTextureCoordinate).xy;
}