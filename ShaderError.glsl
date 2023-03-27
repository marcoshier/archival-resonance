#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
out vec4 o_color;

uniform vec4 color;

void main() {
    vec3 c = vec3(0.4);
    vec3 fill = texture(tex0, v_texCoord0).xyz;
    
    if(any(lessThan(fill, color))) {
          c = fill.xyz;
    } else {
          c = vec3(0.5).xyz;
    }
    
    o_color = vec4(fill, 1.0);
}
// -------------
// color-range-shader
// created 2023-03-19T12:20:26.346143665
/*
0(13) : error C7011: implicit cast from "vec4" to "vec3"
*/
