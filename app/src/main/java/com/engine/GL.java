package com.engine;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.io.InputStream;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLUtils;

public class GL {
	public static final float[] VERMELHO = {1f, 0f, 0f, 1f};
	public static final float[] VERDE = {0f, 1f, 0f, 1f};
	public static final float[] AZUL = {0f, 0f, 1f, 1f};
	// gerais:
	public static void limpar() {
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
	}

	public static void corFundo(float... cor) {
		GLES30.glClearColor(cor[0], cor[1], cor[2], cor[3]);
	}

	public static void defRender(GLSurfaceView tela, GLSurfaceView.Renderer render) {
        tela.setEGLContextClientVersion(3);
        tela.setRenderer(render);
        tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

	public static void ativar3D(boolean ativo) {
		if(ativo) {
			GLES30.glEnable(GLES30.GL_DEPTH_TEST);
			GLES30.glEnable(GLES30.GL_CULL_FACE);
		} else {
			GLES30.glDisable(GLES30.GL_DEPTH_TEST);
			GLES30.glDisable(GLES30.GL_CULL_FACE);
		}
	}

	public static void ativarTransparente(boolean ativo) {
		if(ativo) GLES30.glEnable(GLES30.GL_BLEND);
		else GLES30.glDisable(GLES30.GL_BLEND);
	}

	public static void ajustarTela(int largura, int altura) {
		GLES30.glViewport(0, 0, largura, altura);
	}

	// memoria:
	public static void ativarVBO(int vbo, int stride) {
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
		GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0);
		GLES30.glEnableVertexAttribArray(0);
		GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 12);
		GLES30.glEnableVertexAttribArray(1);
	}

	public static int gerarVBO(float[] vertices) {
		FloatBuffer buffer = criarFloatBuffer(vertices.length);
		buffer.put(vertices).position(0);
		return gerarVBO(buffer);
	}

	public static int gerarVBO(FloatBuffer buffer) {
		int[] vbo = new int[1];
		GLES30.glGenBuffers(1, vbo, 0);
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES30.GL_STATIC_DRAW);

		return vbo[0];
	}

	public static int gerarVAO(int vbo) {
		return gerarVAO(vbo, 5 * 4);
	}

	public static int gerarVAO(int vbo, int stride) {
		int[] vao = new int[1];
		GLES30.glGenVertexArrays(1, vao, 0);

		GLES30.glBindVertexArray(vao[0]);

		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
		GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0);
		GLES30.glEnableVertexAttribArray(0);

		GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 12);
		GLES30.glEnableVertexAttribArray(1);

		GLES30.glBindVertexArray(0);

		return vao[0];
	}

	public static int gerarIBO(short[] indices) {
		ShortBuffer buffer = criarShortBuffer(indices.length);
		buffer.put(indices).position(0);
		return gerarIBO(buffer);
	}

	public static int gerarIBO(ShortBuffer buffer) {
		int[] ibo = new int[1];
		GLES30.glGenBuffers(1, ibo, 0);
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
		GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffer.capacity() * 2, buffer, GLES30.GL_STATIC_DRAW);
		return ibo[0];
	}

	public static FloatBuffer criarFloatBuffer(int tamanho) {
		FloatBuffer buffer = ByteBuffer.allocateDirect(tamanho * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();

		return buffer;
	}

	public static ShortBuffer criarShortBuffer(int tamanho) {
		return ByteBuffer.allocateDirect(tamanho * 2)
			.order(ByteOrder.nativeOrder())
			.asShortBuffer();
	}

	public static ByteBuffer criarByteBuffer(int tamanho) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(tamanho * 4)
            .order(ByteOrder.nativeOrder());
        return buffer;
	}
	
	// shaders:
	public static int criarPrograma(String vert, String frag) {
        int vs = compilar(GLES30.GL_VERTEX_SHADER, vert);
        int fs = compilar(GLES30.GL_FRAGMENT_SHADER, frag);

        int programa = GLES30.glCreateProgram();
        GLES30.glAttachShader(programa, vs);
        GLES30.glAttachShader(programa, fs);
        GLES30.glLinkProgram(programa);

		int[] status = new int[1];
		GLES30.glGetProgramiv(programa, GLES30.GL_LINK_STATUS, status, 0);
		if(status[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(programa);                
            System.out.println("erro ao linkar programa:\n" + log);
        }
		GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
		status = null;
        return programa;
    }

    public static int compilar(int tipo, String fonte) {
        int shader = GLES30.glCreateShader(tipo);
        GLES30.glShaderSource(shader, fonte);
        GLES30.glCompileShader(shader);
        int[] status = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);
        if(status[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            System.out.println("erro compilando shader: " + log);
        }
        return shader;
    }

	public static String lerShaderRaw(Context ctx, int resId) {
        try {
            InputStream is = ctx.getResources().openRawResource(resId);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer);
        } catch(Exception e) {
            return null;
        }
    }

	public static void usar(int idPrograma) {
        GLES30.glUseProgram(idPrograma);
    }

    public static String obterVert2D() {
        return
            "#version 300 es\n"+ 
            "layout(location = 0) in vec2 aPos;\n"+
            "layout(location = 1) in vec2 aTex;\n"+
            "uniform mat4 uProjecao;\n"+
            "out vec2 vTex;\n"+
            "void main() {\n"+
            "gl_Position = uProjecao * vec4(aPos, 0.0, 1.0);\n"+
            "vTex = aTex;\n"+
            "}";
    }

    public static String obterFrag2D() {
		return
			"#version 300 es\n"+
			"precision mediump float;\n"+
			"in vec2 vTex;\n"+
			"uniform sampler2D uTextura;\n"+
			"out vec4 fragCor;\n"+
			"void main() {\n"+
			"vec4 cor = texture(uTextura, vTex);\n"+
			"if(cor.a < 0.01) discard;\n"+  // ignora pixels totalmente transparentes
			"fragCor = cor;\n"+
			"}";
	}

	public static String obterVert3D() {
		return
			"#version 300 es\n"+
			"layout(location = 0) in vec3 aPos;\n"+
			"layout(location = 1) in vec2 aTex;\n"+
			"uniform mat4 uMVP;\n"+
			"out vec2 vTex;\n"+
			"void main() {\n"+
			"gl_Position = uMVP * vec4(aPos, 1.0);\n"+
			"vTex = aTex;\n"+
			"}";
	}

	public static String obterFrag3D() {
		return
			"#version 300 es\n"+
			"precision mediump float;\n"+
			"in vec2 vTex;\n"+
			"uniform sampler2D uTextura;\n"+
			"out vec4 fragCor;\n"+
			"void main() {\n"+
			"vec4 cor = texture(uTextura, vTex);\n"+
			"if(cor.a < 0.5) discard;\n"+
			"fragCor = cor;\n"+
			"}";
	}
	
	public static class VBOGrupo {
		public int texturaId;
		public int vboId;
		public int iboId;
		public int vertices;

		public VBOGrupo(int texturaId, int vboId, int iboId, int vertices) {
			this.texturaId = texturaId;
			this.vboId = vboId;
			this.iboId = iboId;
			this.vertices = vertices;
		}
	}
	
	// texturas:
	public static int carregarTexturaAsset(Context ctx, String nomeArquivo) {
		try {
			Bitmap bmp = ArmUtils.lerImgAssets(ctx, nomeArquivo);
			int[] texID = new int[1];
			GLES30.glGenTextures(1, texID, 0);
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texID[0]);

			GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);

			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

			bmp.recycle();
			return texID[0];
		} catch(Exception e) {
			System.out.println("erro ao carregar textura: " + nomeArquivo + e.getMessage());
			return -1;
		}
	}

	public static int texturaBranca() {
        int[] tex = new int[1];
        GLES30.glGenTextures(1, tex, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0]);
        byte[] branco = {(byte)255, (byte)255, (byte)255, (byte)255};
        ByteBuffer buffer = ByteBuffer.allocateDirect(4);
        buffer.put(branco).position(0);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0,  GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

        return tex[0];
    }

	public static int texturaCor(float... c) {
		int[] tex = new int[1];
		GLES30.glGenTextures(1, tex, 0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0]);

		byte[] cor = {
			(byte)(c[0] * 255), (byte)(c[1] * 255), (byte)(c[2] * 255), (byte)(c[3] * 255)
		};

		ByteBuffer buffer = ByteBuffer.allocateDirect(4);
		buffer.put(cor).position(0);

		GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0,  
							GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);

		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
		return tex[0];
	}
}

