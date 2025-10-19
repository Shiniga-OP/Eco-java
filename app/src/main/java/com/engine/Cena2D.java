package com.engine;

import java.util.List;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import java.util.Arrays;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.opengl.GLUtils;
import android.graphics.Color;

public class Cena2D {
    public int vao, vbo;
    public int shader;
    public float[] matrizProj;
    public int locProjecao, locTexture;

    public List<Objeto2D> objetos;
	public List<Botao2D> botoes;

    public FloatBuffer bufferTemp;
	
	public float larguraTela, alturaTela;
	
	public void iniciar() {
		iniciar(GL.obterVert2D(), GL.obterFrag2D());
	}

    public void iniciar(String sv, String sf) {
		matrizProj = new float[16];
		objetos = new ArrayList<>();
		botoes = new ArrayList<>();
		bufferTemp = GL.criarFloatBuffer(4 * 4);
        shader = GL.criarPrograma(sv, sf);
        int[] ids = new int[1];
        GLES30.glGenVertexArrays(1, ids, 0);
        vao = ids[0];
        GLES30.glBindVertexArray(vao);

        GLES30.glGenBuffers(1, ids, 0);             
        vbo = ids[0];      
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 4 * 4 *4, null, GLES30.GL_STATIC_DRAW);   

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 4 *4, 0);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 4 *4, 2 *4);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glBindVertexArray(0);

        locProjecao = GLES30.glGetUniformLocation(shader, "uProjecao");
        locTexture = GLES30.glGetUniformLocation(shader, "uTextura");
    }

    public void render() {
        GL.usar(shader);
        GLES30.glUniformMatrix4fv(locProjecao, 1, false, matrizProj, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glUniform1i(locTexture, 0);

        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        for(Objeto2D o : objetos) {
			float x = o.x / larguraTela;
			float y = o.y / alturaTela;
			float largura = o.largura / larguraTela;
			float altura = o.altura / alturaTela;

			bufferTemp.clear();
			bufferTemp.put(x).put(y).put(0f).put(0f);
			bufferTemp.put(x).put(y + altura).put(0f).put(1f);
			bufferTemp.put(x + largura).put(y).put(1f).put(0f);
			bufferTemp.put(x + largura).put(y + altura).put(1f).put(1f);
			bufferTemp.flip();

			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, o.textura);
			GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, bufferTemp.remaining() * 4, bufferTemp);
			GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
		}
        GLES30.glBindVertexArray(0);
    }

	public void atualizarProjecao(int h, int v) {
		larguraTela = h;
		alturaTela = v;
		Matrix.orthoM(matrizProj, 0, 0, 1f, 1f, 0, -1, 1);
	}

	public void add(Objeto2D... os) {
        for(int i = 0; i < os.length; i++) objetos.add(os[i]);
    }
	
	public void add(Botao2D... os) {
        for(int i = 0; i < os.length; i++) {
			botoes.add(os[i]);
			objetos.add(os[i].objeto);
		}
    }
	
	public void add(Texto2D... os) {
        for(int i = 0; i < os.length; i++) {
			objetos.add(os[i].objeto);
		}
    }

	public void rm(Objeto2D... os) {
		for(int i = 0; i < os.length; i++) objetos.remove(os[i]);
	}
	
	public void rm(Texto2D... os) {
		for(int i = 0; i < os.length; i++) objetos.remove(os[i].objeto);
	}
	
	public void rm(Botao2D... os) {
		for(int i = 0; i < os.length; i++) {
			objetos.remove(os[i].objeto);
			botoes.remove(os[i]);
		}
	}
	
	public static class Objeto2D {
		public float x, y, largura, altura;
		public int textura;

		public Objeto2D(float x, float y, float largura, float altura, int textura) {
			this.x = x;
			this.y = y;
			this.largura = largura;
			this.altura = altura;
			this.textura = (textura == -1) ? GL.texturaBranca() : textura;
		}

		public boolean tocado(float x, float y) {
			if(
				x >= this.x &&
				x <= this.x + this.largura &&
				y >= this.y &&
				y <= this.y + this.altura
				) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	public static class Botao2D {
		public Objeto2D objeto;
		public boolean pressionado = false;
		public Handler repetidor;
		public Runnable acao;
		public int pontoAtivo = -1;

		public Botao2D(Objeto2D objeto2D) {
			this.objeto = objeto2D;
			this.repetidor = new Handler(Looper.getMainLooper());
		}

		public void definirAcao(Runnable acao) {
			this.acao = acao;
		}

		public boolean verificarToque(MotionEvent e) {
			int tipo = e.getActionMasked();
			int quant = e.getPointerCount();

			switch(tipo) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_POINTER_DOWN:
					if(pontoAtivo == -1) {
						for(int i = 0; i < quant && i < 2; i++) {
							float x = e.getX(i), y = e.getY(i);
							if(objeto.tocado(x, y)) {
								pontoAtivo = e.getPointerId(i);
								pressionado = true;
								iniciarRepeticao();
								return true;
							}
						}
					}
					break;
				case MotionEvent.ACTION_MOVE:
					for(int i = 0; i < quant; i++) {
						if(e.getPointerId(i) == pontoAtivo) {
							float x = e.getX(i), y = e.getY(i);
							if(!objeto.tocado(x, y)) {
								pressionado = false;
								pontoAtivo = -1;
								pararRepeticao();
							}
							return true;
						}
					}
					break;
				case MotionEvent.ACTION_POINTER_UP:
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					int id = e.getPointerId(e.getActionIndex());
					if(id == pontoAtivo) {
						pressionado = false;
						pontoAtivo = -1;
						pararRepeticao();
					}
					break;
			}
			return pressionado;
		}

		public void iniciarRepeticao() {
			repetidor.postDelayed(new Runnable() {
					@Override
					public void run() {
						if(pressionado && acao != null) {
							acao.run();
							repetidor.postDelayed(this, 100);
						}
					}
				}, 0);
		}

		public void pararRepeticao() {
			repetidor.removeCallbacksAndMessages(null);
		}
	}
	
	public static class Texto2D {
		public Objeto2D objeto;
		public int textura = -1;
		public String texto;
		public float x, y;
		public float fonteTam;
		public float[] cor;

		public Texto2D(String texto, float x, float y, float fonteTam, float... cor) {
			this.texto = (texto == "") ? "." : texto;
			this.x = x;
			this.y = y;
			this.fonteTam = fonteTam;
			this.cor = cor.clone();
			gerarRecursos();
		}

		public void gerarRecursos() {
			if(textura != -1) {
				GLES30.glDeleteTextures(1, new int[]{textura}, 0);
			}

			textura = criarTexturaBitmap();
			criarObjeto2D();
		}

		public int criarTexturaBitmap() {
			Paint pincel = new Paint();
			pincel.setTextSize(fonteTam);
			pincel.setColor(Color.WHITE);
			pincel.setAntiAlias(true);
			pincel.setTextAlign(Paint.Align.LEFT);

			Rect limites = new Rect();
			pincel.getTextBounds(texto, 0, texto.length(), limites);
			float largura = pincel.measureText(texto);
			float altura = limites.height();

			Bitmap bitmap = Bitmap.createBitmap(
				(int) Math.ceil(largura), 
				(int) Math.ceil(altura), 
				Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			canvas.drawText(texto, 0, -limites.top, pincel);

			int rgb = Color.argb(
				(int)(cor[3] * 255), 
				(int)(cor[0] * 255), 
				(int)(cor[1] * 255), 
				(int)(cor[2] * 255));
			bitmap = aplicarCor(bitmap, rgb);

			int[] texId = new int[1];
			GLES30.glGenTextures(1, texId, 0);
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId[0]);
			GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

			bitmap.recycle();
			return texId[0];
		}

		public Bitmap aplicarCor(Bitmap original, int cor) {
			Bitmap resultado = Bitmap.createBitmap(
				original.getWidth(), 
				original.getHeight(), 
				Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(resultado);
			Paint pincel = new Paint();
			pincel.setColorFilter(new PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_IN));
			canvas.drawBitmap(original, 0, 0, pincel);
			return resultado;
		}

		public void criarObjeto2D() {
			Paint pincel = new Paint();
			pincel.setTextSize(fonteTam);
			Rect limites = new Rect();
			pincel.getTextBounds(texto, 0, texto.length(), limites);

			objeto = new Objeto2D(
				x, y, 
				pincel.measureText(texto), 
				limites.height(), 
				textura);
		}

		public void defTexto(String novoTexto) {
			if(!texto.equals(novoTexto)) {
				texto = novoTexto;
				gerarRecursos();
			}
		}

		public void defCor(float[] novaCor) {
			if(!Arrays.equals(cor, novaCor)) {
				cor = novaCor.clone();
				gerarRecursos();
			}
		}

		public void definirTam(float novoTamanho) {
			if(fonteTam != novoTamanho) {
				fonteTam = novoTamanho;
				gerarRecursos();
			}
		}

		public void defPos(float x, float y) {
			this.x = x;
			this.y = y;
			if(objeto != null) {
				objeto.x = x;
				objeto.y = y;
			}
		}

		public void destruir() {
			if(textura != -1) {
				GLES30.glDeleteTextures(1, new int[]{textura}, 0);
				textura = -1;
			}
		}
	}
}
