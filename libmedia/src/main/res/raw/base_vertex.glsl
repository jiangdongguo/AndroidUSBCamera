attribute vec4 aPosition;
attribute vec4 aTextureCoordinate;
varying vec2 vTextureCoord;
void main()
{
    vTextureCoord = aTextureCoordinate.xy;
    gl_Position = aPosition;
}