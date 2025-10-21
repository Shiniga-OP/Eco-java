package com.engine;

import java.nio.FloatBuffer;
import java.util.List;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import android.opengl.GLES30;
import android.opengl.Matrix;

public class Cena3D {
	public int programa;
	public int locMVP;
	public int locTextura;

	public List<Objeto3D> objetos;
	public Camera3D camera = new Camera3D();
	public float[] matrizV;
	public float[] matrizProj;
	public float[] matrizTemp;
	
	public void iniciar() {
		iniciar(GL.obterVert3D(), GL.obterFrag3D());
	}

	public void iniciar(String sv, String sf) {
		objetos = new ArrayList<>();
		matrizV = new float[16];
		matrizProj = new float[16];
		matrizTemp = new float[16];

		programa = GL.criarPrograma(sv, sf);
		locMVP = GLES30.glGetUniformLocation(programa, "uMVP");
		locTextura = GLES30.glGetUniformLocation(programa, "uTextura");

		Matrix.setIdentityM(matrizV, 0);
		Matrix.setIdentityM(matrizProj, 0);
	}

	public void attCamera(Camera3D c) {
        Matrix.setLookAtM(matrizV, 0,
		c.pos[0], c.pos[1], c.pos[2],
		c.pos[0] + c.foco[0],
		c.pos[1] + c.foco[1],
		c.pos[2] + c.foco[2],
		c.cima[0], c.cima[1], c.cima[2]);
    }
	
	public void attCamera() {
		attCamera(this.camera);
	}

	public void defPerspectiva(float zoom, float aspecto, float perto, float longe) {
		Matrix.perspectiveM(matrizProj, 0, zoom, aspecto, perto, longe);
	}

	public void renderizar() {
		GL.usar(programa);
		GLES30.glUniform1i(locTextura, 0);

		for(Objeto3D modelo : objetos) {
			if(!modelo.visivel) continue;

			Matrix.multiplyMM(matrizTemp, 0, matrizV, 0, modelo.matrizT, 0);
			Matrix.multiplyMM(matrizTemp, 0, matrizProj, 0, matrizTemp, 0);

			GLES30.glUniformMatrix4fv(locMVP, 1, false, matrizTemp, 0);
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, modelo.textura);

			GLES30.glBindVertexArray(modelo.vao);
			GLES30.glDrawElements(GLES30.GL_TRIANGLES, modelo.quantIndices, GLES30.GL_UNSIGNED_SHORT, 0);
			GLES30.glBindVertexArray(0);
		}
	}

	public void add(Objeto3D... objs) {
		for(int i = 0; i < objs.length; i++) {
			objetos.add(objs[i]);
		}
	}

	public void rm(Objeto3D... objs) {
		for(int i = 0; i < objs.length; i++) {
			objetos.remove(objs[i]);
		}
	}

	public static class Objeto3D {
		public int vao;
		public int vbo;
		public int ibo;
		public int textura;
		public int quantIndices;
		public float[] matrizT = new float[16];
		public boolean visivel;

		public Objeto3D(float[] vertices, short[] indices, int texturaId) {
			Matrix.setIdentityM(matrizT, 0);
			visivel = true;
			textura = texturaId;
			quantIndices = indices.length;

			int[] buffers = new int[3];
			GLES30.glGenVertexArrays(1, buffers, 0);
			vao = buffers[0];
			GLES30.glGenBuffers(2, buffers, 1);
			vbo = buffers[1];
			ibo = buffers[2];

			GLES30.glBindVertexArray(vao);

			FloatBuffer vertBuffer = GL.criarFloatBuffer(vertices.length);
			vertBuffer.put(vertices).position(0);

			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
			GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertBuffer.capacity() * 4, vertBuffer, GLES30.GL_STATIC_DRAW);

			ShortBuffer indBuffer = GL.criarShortBuffer(indices.length);
			indBuffer.put(indices).position(0);

			GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo);
			GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indBuffer.capacity() * 2, indBuffer, GLES30.GL_STATIC_DRAW);

			GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0);
			GLES30.glEnableVertexAttribArray(0);
			GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 3 * 4);
			GLES30.glEnableVertexAttribArray(1);

			GLES30.glBindVertexArray(0);
		}

		public void defPos(float x, float y, float z) {
			Matrix.translateM(matrizT, 0, x, y, z);
		}

		public void rotacao(float angulo, float x, float y, float z) {
			Matrix.rotateM(matrizT, 0, angulo, x, y, z);
		}

		public void escalar(float x, float y, float z) {
			Matrix.scaleM(matrizT, 0, x, y, z);
		}

		public void destruir() {
			int[] buffers = {vbo, ibo};
			GLES30.glDeleteBuffers(2, buffers, 0);
			GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
		}
	}
	
	public static class Camera3D {
		public float[] pos = {0, 0, 0};
		public float[] foco = {0, 0, -1};
		public float[] cima = {0, 1, 0};

		public float yaw = -90;
		public float tom = 0;

		public Camera3D() {
			this.rotacionar(0, 0);
		}

		public void rotacionar(float dx, float dy) {
			// rotacao invertida propositalmente para rotacao certa:
			yaw += dx;
			tom -= dy;

			if(tom > 89) tom = 89;
			if(tom < -89) tom = -89;

			foco[0] = (float)(Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(tom)));
			foco[1] = (float)Math.sin(Math.toRadians(tom));
			foco[2] = (float)(Math.sin(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(tom)));
			normalize(foco);
		}

		public void normalize(float[] v) {
			float tamanho = (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
			if(tamanho==0) return;
			v[0] /= tamanho;
			v[1] /= tamanho;
			v[2] /= tamanho;
		}
	}

	public static class Cubo3D extends Objeto3D {
		public static float[] verticesCubo = {
			// frente
			-0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
			0.5f, -0.5f,  0.5f,  1.0f, 0.0f,
			0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
			-0.5f,  0.5f,  0.5f,  0.0f, 1.0f,

			// tras
			-0.5f, -0.5f, -0.5f,  1.0f, 0.0f,
			-0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
			0.5f,  0.5f, -0.5f,  0.0f, 1.0f,
			0.5f, -0.5f, -0.5f,  0.0f, 0.0f,

			// cima
			-0.5f,  0.5f, -0.5f,  0.0f, 1.0f,
			-0.5f,  0.5f,  0.5f,  0.0f, 0.0f,
			0.5f,  0.5f,  0.5f,  1.0f, 0.0f,
			0.5f,  0.5f, -0.5f,  1.0f, 1.0f,

			// baixo
			-0.5f, -0.5f, -0.5f,  1.0f, 1.0f,
			0.5f, -0.5f, -0.5f,  0.0f, 1.0f,
			0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
			-0.5f, -0.5f,  0.5f,  1.0f, 0.0f,

			// direita
			0.5f, -0.5f, -0.5f,  1.0f, 0.0f,
			0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
			0.5f,  0.5f,  0.5f,  0.0f, 1.0f,
			0.5f, -0.5f,  0.5f,  0.0f, 0.0f,

			// esquerda
			-0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
			-0.5f, -0.5f,  0.5f,  1.0f, 0.0f,
			-0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
			-0.5f,  0.5f, -0.5f,  0.0f, 1.0f
		};

		public static short[] indicesCubo = {
			0, 1, 2,  0, 2, 3,    // frente
			4, 5, 6,  4, 6, 7,    // tras
			8, 9, 10, 8, 10, 11,  // cima
			12, 13, 14, 12, 14, 15, // baixo
			16, 17, 18, 16, 18, 19, // direita
			20, 21, 22, 20, 22, 23  // esquerda
		};

		public Cubo3D(int texturaId) {
			super(verticesCubo, indicesCubo, texturaId);
		}
	}
}
