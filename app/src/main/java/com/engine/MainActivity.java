package com.engine;
 
import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import android.view.MotionEvent;

public class MainActivity extends Activity implements GLSurfaceView.Renderer {
	Ambiente3D cena = new Ambiente3D();
	Ambiente3D.Cubo3D cubo;
	Ambiente3D.Luz luz = new Ambiente3D.Luz(0, 1.5f, -3, 100f, 0, 0, 1, 5);
	int v = 100;
	int h = 100;
	
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.inicio);
		GLSurfaceView tela = findViewById(R.id.tela);
		GL.defRender(tela, this);
    }
	
	public float angulo = 0f;
	public final float velo = 2f;

	@Override
	public void onDrawFrame(GL10 gl) {
		GL.limpar();
		cena.attCamera();

		angulo += velo;
		if(angulo > 360f) angulo -= 360f;

		float radianos = (float)Math.toRadians(angulo);
		float raio = 1.0f;

		luz.pos[0] = (float)Math.cos(radianos) * raio;
		luz.pos[1] = (float)Math.sin(radianos * 0.5f) * 0.5f;
		luz.pos[2] = (float)Math.sin(radianos) * raio;
		
		cubo.rotacao(5f, 10f, 0, 5f);

		cena.renderizar();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int v, int h) {
		GL.ajustarTela(v, h);
		cena.defPerspectiva(45f, (float)v/h, 0.1f, 100f);
		GL.corFundo(0.1f, 0.1f, 0.1f, 1f); // cinza
		this.v = v;
		this.h = h;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GL.ativar3D(true);
		cena.iniciar();
		
		cubo = new Ambiente3D.Cubo3D(GL.texturaBranca());
		cena.add(cubo);
		cubo.defPos(0, 0, -3f);
		cena.luzes.add(luz);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return Toque.cameraOlhar(cena.camera, e); // permite movimentar a camera com  o arrastar
	} 
}
