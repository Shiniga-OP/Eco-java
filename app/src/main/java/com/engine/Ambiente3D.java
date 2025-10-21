package com.engine;

import android.opengl.Matrix;
import android.opengl.GLES30;
import java.nio.ShortBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.ArrayList;

public class Ambiente3D {
    public static final int MAX_LUZES = 8;

    public int programa;
    public int locMVP;
    public int locModelo;
    public int locPosV;
    public int locNumLuzes;
    public int locLuzPos;
    public int locLuzCor;
    public int locLuzRaios;
    public int locTextura;

    public final List<Objeto3D> objetos = new ArrayList<Objeto3D>();
    public final List<Luz> luzes = new ArrayList<Luz>();
    public Camera3D camera = new Camera3D();
	
    public final float[] matrizV = new float[16];
    public final float[] matrizProj = new float[16];
    public final float[] matrizTemp = new float[16];
    public final float[] matrizMV = new float[16]; // visual * modelo temp
	// tmp
	public float[] posArr;
	public float[] corArr;
	public float[] raiosArr;

    public void iniciar() {
        iniciar(obterVert3D(), obterFrag3D());
    }

    public void iniciar(String sv, String sf) {
        programa = GL.criarPrograma(sv, sf);
        locMVP = GLES30.glGetUniformLocation(programa, "uMVP");
        locModelo = GLES30.glGetUniformLocation(programa, "uModelo");
        locPosV = GLES30.glGetUniformLocation(programa, "uPosV");
        locNumLuzes = GLES30.glGetUniformLocation(programa, "uNumLuzes");
        // pega local da primeira posição do array
        locLuzPos = GLES30.glGetUniformLocation(programa, "uLuzPos[0]");
        locLuzCor = GLES30.glGetUniformLocation(programa, "uLuzCor[0]");
        locLuzRaios = GLES30.glGetUniformLocation(programa, "uLuzRaios[0]");
        locTextura = GLES30.glGetUniformLocation(programa, "uTextura");
		
		posArr = new float[MAX_LUZES * 3];
        corArr = new float[MAX_LUZES * 3];
        raiosArr = new float[MAX_LUZES];
		
        Matrix.setIdentityM(matrizV, 0);
        Matrix.setIdentityM(matrizProj, 0);
    }

    public void attCamera(Camera3D c) {
        Matrix.setLookAtM(matrizV, 0,
		c.pos[0], c.pos[1], c.pos[2], c.pos[0] + c.foco[0],
		c.pos[1] + c.foco[1], c.pos[2] + c.foco[2],
		c.cima[0], c.cima[1], c.cima[2]);
    }

    public void attCamera() {
        attCamera(this.camera);
    }

    public void defPerspectiva(float zoom, float aspecto, float perto, float longe) {
        Matrix.perspectiveM(matrizProj, 0, zoom, aspecto, perto, longe);
    }
	
	public void renderizar() {
		renderizar(this.objetos);
	}
	
    public void renderizar(List<Objeto3D> objetos) {
        GL.usar(programa);
        GLES30.glUniform1i(locTextura, 0);

        for(Objeto3D o : objetos) {
            if(!o.visivel) continue;

            float objX = o.matrizT[12];
            float objY = o.matrizT[13];
            float objZ = o.matrizT[14];
            // so luz perto
            int usado = 0;
            for(int i = 0; i < luzes.size() && usado < MAX_LUZES; i++) {
                Luz l = luzes.get(i);
                float dx = l.pos[0] - objX;
                float dy = l.pos[1] - objY;
                float dz = l.pos[2] - objZ;
                float dist2 = dx*dx + dy*dy + dz*dz;
                float lim = (l.raio + o.raioColisao);
                if(dist2 <= lim*lim) {
                    int base = usado * 3;
                    posArr[base + 0] = l.pos[0];
                    posArr[base + 1] = l.pos[1];
                    posArr[base + 2] = l.pos[2];

                    corArr[base + 0] = l.cor[0] * l.nivel;
                    corArr[base + 1] = l.cor[1] * l.nivel;
                    corArr[base + 2] = l.cor[2] * l.nivel;

                    raiosArr[usado] = l.raio;
                    usado++;
                }
            }
            // MVP:proj * visual * modelo
            Matrix.multiplyMM(matrizMV, 0, matrizV, 0, o.matrizT, 0); // visual * modelo
            Matrix.multiplyMM(matrizTemp, 0, matrizProj, 0, matrizMV, 0); // proj * visual * modelo

            GLES30.glUniformMatrix4fv(locMVP, 1, false, matrizTemp, 0);
            GLES30.glUniformMatrix4fv(locModelo, 1, false, o.matrizT, 0);
            // posição da camera PosV)
            GLES30.glUniform3f(locPosV, camera.pos[0], camera.pos[1], camera.pos[2]);
            // luzes(num + arrays)
            GLES30.glUniform1i(locNumLuzes, usado);
            if(usado > 0) {
                GLES30.glUniform3fv(locLuzPos, usado, posArr, 0);
                GLES30.glUniform3fv(locLuzCor, usado, corArr, 0);
                GLES30.glUniform1fv(locLuzRaios, usado, raiosArr, 0);
            }
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, o.textura);
            GLES30.glBindVertexArray(o.vao);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, o.quantIndices, GLES30.GL_UNSIGNED_SHORT, 0);
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
    // pos 3, uv 2, normal 3, 8 floats por vertice
    public static class Objeto3D {
        public int vao;
        public int vbo;
        public int ibo;
        public int textura;
        public int quantIndices;
        public float[] matrizT = new float[16];
        public boolean visivel;
        public float raioColisao = 1.0f; // pra culling de luzes

        public Objeto3D(float[] vertices, short[] indices, int texturaId, float raio) {
            Matrix.setIdentityM(matrizT, 0);
            visivel = true;
            textura = texturaId;
            quantIndices = indices.length;
            if(raio > 0f) raioColisao = raio;

            int[] vaoIds = new int[1];
            GLES30.glGenVertexArrays(1, vaoIds, 0);
            vao = vaoIds[0];

            int[] bufferIds = new int[2];
            GLES30.glGenBuffers(2, bufferIds, 0);
            vbo = bufferIds[0];
            ibo = bufferIds[1];

            GLES30.glBindVertexArray(vao);

            FloatBuffer vertBuffer = GL.criarFloatBuffer(vertices.length);
            vertBuffer.put(vertices).position(0);

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertBuffer.capacity() * 4, vertBuffer, GLES30.GL_STATIC_DRAW);

            ShortBuffer indBuffer = GL.criarShortBuffer(indices.length);
            indBuffer.put(indices).position(0);

            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indBuffer.capacity() * 2, indBuffer, GLES30.GL_STATIC_DRAW);

            final int stride = 8 * 4; // 8 floats * 4 bytes
            // pos loc=0
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0);
            GLES30.glEnableVertexAttribArray(0);
            // uv loc=1 
            GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 3 * 4);
            GLES30.glEnableVertexAttribArray(1);
            // normal loc=2 
            GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, (3 + 2) * 4);
            GLES30.glEnableVertexAttribArray(2);

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

    public static class Cubo3D extends Objeto3D {
        public static float[] verticesCubo = {
            // frente(normal 0,0,1)
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f,  0f, 0f, 1f,
			0.5f, -0.5f,  0.5f,  1.0f, 0.0f,  0f, 0f, 1f,
			0.5f,  0.5f,  0.5f,  1.0f, 1.0f,  0f, 0f, 1f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f,  0f, 0f, 1f,
            // tras(0,0,-1)
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f,  0f, 0f, -1f,
            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f,  0f, 0f, -1f,
			0.5f,  0.5f, -0.5f,  0.0f, 1.0f,  0f, 0f, -1f,
			0.5f, -0.5f, -0.5f,  0.0f, 0.0f,  0f, 0f, -1f,
            // cima(0,1,0)
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f,  0f, 1f, 0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 0.0f,  0f, 1f, 0f,
			0.5f,  0.5f,  0.5f,  1.0f, 0.0f,  0f, 1f, 0f,
			0.5f,  0.5f, -0.5f,  1.0f, 1.0f,  0f, 1f, 0f,
            // baixo(0,-1,0)
            -0.5f, -0.5f, -0.5f,  1.0f, 1.0f,  0f, -1f, 0f,
			0.5f, -0.5f, -0.5f,  0.0f, 1.0f,  0f, -1f, 0f,
			0.5f, -0.5f,  0.5f,  0.0f, 0.0f,  0f, -1f, 0f,
            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f,  0f, -1f, 0f,
            // direita(1,0,0)
			0.5f, -0.5f, -0.5f,  1.0f, 0.0f,  1f, 0f, 0f,
			0.5f,  0.5f, -0.5f,  1.0f, 1.0f,  1f, 0f, 0f,
			0.5f,  0.5f,  0.5f,  0.0f, 1.0f,  1f, 0f, 0f,
			0.5f, -0.5f,  0.5f,  0.0f, 0.0f,  1f, 0f, 0f,
            // esquerda(-1,0,0)
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,  -1f, 0f, 0f,
            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f,  -1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f,  1.0f, 1.0f,  -1f, 0f, 0f,
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f,  -1f, 0f, 0f
        };

        public static short[] indicesCubo = {
            0,1,2, 0,2,3,
            4,5,6, 4,6,7,
            8,9,10, 8,10,11,
            12,13,14, 12,14,15,
            16,17,18, 16,18,19,
            20,21,22, 20,22,23
        };

        public Cubo3D(int texturaId) {
            super(verticesCubo, indicesCubo, texturaId, 0.866f); // raio aproximado
        }
    }

    public static class Camera3D {
        public float[] pos = {1, 1.7f, 1};
        public float[] foco = {0, 0, -1};
        public float[] cima = {0, 1, 0};

        public float yaw = -90;
        public float tom = 0;

        public Camera3D() {
            this.rotacionar(0, 0);
        }

        public void rotacionar(float dx, float dy) {
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
            if (tamanho==0) return;
            v[0] /= tamanho;
            v[1] /= tamanho;
            v[2] /= tamanho;
        }
    }

    public static String obterVert3D() {
        return
            "#version 300 es\n" +
            "layout(location = 0) in vec3 aPos;\n" +
            "layout(location = 1) in vec2 aTex;\n" +
            "layout(location = 2) in vec3 aNormal;\n" +
            "uniform mat4 uMVP;\n" +
            "uniform mat4 uModelo;\n" +
            "out vec2 vTex;\n" +
            "out vec3 vNormal;\n" +
            "out vec3 vMundoPos;\n" +
            "void main() {\n" +
            "    vec4 mundoPos = uModelo * vec4(aPos, 1.0);\n" +
            "    vMundoPos = mundoPos.xyz;\n" +
            "    vNormal = mat3(uModelo) * aNormal;\n" +
            "    vTex = aTex;\n" +
            "    gl_Position = uMVP * vec4(aPos, 1.0);\n" +
            "}\n";
    }

    public static String obterFrag3D() {
        return
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 vTex;\n" +
            "in vec3 vNormal;\n" +
            "in vec3 vMundoPos;\n" +
            "uniform sampler2D uTextura;\n" +
            "uniform int uNumLuzes;\n" +
            "uniform vec3 uLuzPos[" + MAX_LUZES + "];\n" +
            "uniform vec3 uLuzCor[" + MAX_LUZES + "];\n" +
            "uniform float uLuzRaios[" + MAX_LUZES + "];\n" +
            "uniform vec3 uPosV;\n" +
            "out vec4 fragCor;\n" +
            "void main() {\n" +
            "    vec4 tex = texture(uTextura, vTex);\n" +
            "    if(tex.a < 0.5) discard;\n" +
            "    vec3 N = normalize(vNormal);\n" +
            "    vec3 V = normalize(uPosV - vMundoPos);\n" +
            "    vec3 conta = vec3(0.0);\n" +
            "    for(int i = 0; i < uNumLuzes; i++) {\n" +
            "        vec3 Lpos = uLuzPos[i];\n" +
            "        vec3 Lcol = uLuzCor[i];\n" +
            "        float raio = uLuzRaios[i];\n" +
            "        vec3 Ldir = Lpos - vMundoPos;\n" +
            "        float dist = length(Ldir);\n" +
            "        if(dist > raio) continue;\n" +
            "        Ldir = normalize(Ldir);\n" +
			// atenuacao quadrática
            "        float att = 1.0 / (1.0 + 2.0 * dist + 1.0 * dist * dist);\n" +
			// difuso
            "        float diff = max(dot(N, Ldir), 0.0);\n" +
			// especular
            "        vec3 H = normalize(Ldir + V);\n" +
            "        float espec = pow(max(dot(N, H), 0.0), 32.0);\n" +
            "        conta += (diff + 0.2 * espec) * Lcol * att;\n" +
            "    }\n" +
			// ambiente minimo + resultado das luzes multiplicando pela textura
            "    vec3 corFinal = tex.rgb * (0.15 + conta);\n" +
            "    fragCor = vec4(corFinal, tex.a);\n" +
            "}\n";
    }

    public static class Luz {
        public float[] pos = {0,0,0};
        public float[] cor = {1f,1f,1f};
        public float nivel = 1f;
        public float raio = 5f;
        public Luz(float x, float y, float z, float r, float cr, float cg, float cb, float nivel) {
            this.pos[0] = x; this.pos[1] = y; this.pos[2] = z;
            this.raio = r;
            this.cor[0] = cr; this.cor[1] = cg; this.cor[2] = cb;
            this.nivel = nivel;
        }
    }
}
