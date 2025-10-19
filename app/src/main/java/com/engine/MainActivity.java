package com.engine;
 
import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import android.view.MotionEvent;

public class MainActivity extends Activity implements GLSurfaceView.Renderer {
	Cena3D cena = new Cena3D();
	
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.inicio);
		GLSurfaceView tela = findViewById(R.id.tela);
		GL.defRender(tela, this);
    }
	
	@Override
	public void onDrawFrame(GL10 gl) {
		GL.limpar();
		cena.attCamera();
		cena.renderizar();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int v, int h) {
		GL.ajustarTela(v, h);
		cena.defPerspectiva(45f, (float)v/h, 0.1f, 100f);
		GL.corFundo(0.1f, 0.1f, 0.1f, 1f); // cinza
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GL.ativar3D(true);
		cena.iniciar();
		
		Cena3D.Cubo3D cubo = new Cena3D.Cubo3D(GL.texturaBranca());
		cena.add(cubo);
		cubo.defPos(0, 0, -3f);
		cubo.rotacao(5f, 10f, 0, 5f);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return Toque.cameraOlhar(cena.camera, e); // permite movimentar a camera com  o arrastar
	} 
}
