#ifdef GL_ES
precision highp float;
#endif
			
varying vec2 v_texCoord;
uniform sampler2D y_texture;
uniform sampler2D u_texture;
uniform sampler2D v_texture;

uniform int yuvType;//0 代表 I420, 1 代表 NV12
uniform sampler2D SamplerNV12_Y;//NV12數據的Y平面
uniform sampler2D SamplerNV12_UV;//NV12數據的UV平面

 void main()
{

	if (yuvType == 1) {//nv12
		float r, g, b, y, u, v;
		y = texture2D(SamplerNV12_Y, v_texCoord).r;
		u = texture2D(SamplerNV12_UV, v_texCoord).r - 0.5;
		v = texture2D(SamplerNV12_UV, v_texCoord).a - 0.5;
		r = y + 1.13983*v;
		g = y - 0.39465*u - 0.58060*v;
		b = y + 2.03211*u;
		gl_FragColor = vec4(r,g,b, 1);
	}else { // yuv420
		float nx, ny, r, g, b, y, u, v;
		nx = v_texCoord.x;
		ny = v_texCoord.y;
		y = texture2D(y_texture, v_texCoord).r;
		u = texture2D(u_texture, v_texCoord).r;
		v = texture2D(v_texture, v_texCoord).r;

		y = 1.1643 * (y - 0.0625);
		u = u - 0.5;
		v = v - 0.5;

		r = y + 1.5958 * v;
		g = y - 0.39173 * u - 0.81290 * v;
		b = y + 2.017 * u;

		gl_FragColor = vec4(r, g, b, 1.0);
	}
}