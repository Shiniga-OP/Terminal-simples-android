package com.terminal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.OutputStream;
import java.io.PrintStream;
import com.ttt.Executor;
import android.view.View;

public class MainActivity extends Activity {
    public TextView saida;
    public EditText entrada;
    public ScrollView scroll;
    public Executor executor;
    public Handler ui;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.tela);

        saida = findViewById(R.id.saida);
        entrada = findViewById(R.id.entrada);
        scroll  = findViewById(R.id.scroll);
        ui = new Handler(Looper.getMainLooper());

        pedirPermissoes();
        redirecionarSaida();
		
		boolean usaLinker64 = true;
		
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) usaLinker64 = false; // so android 10+ usa linker64
		
        executor = new Executor(getFilesDir(), usaLinker64);
		
		introducao();
    }
	
	public void confirmar(View v) {
		String cmd = entrada.getText().toString().trim();
		if(!cmd.isEmpty()) {
			entrada.setText("");
			
			if(cmd.equals("clear")) {
				saida.setText("");
				return;
			}
			System.out.println("$ " + cmd);
			executor.exec(cmd);
		}
	}

    public void redirecionarSaida() {
		try {
			PrintStream ps = new PrintStream(new OutputStream() {
					@Override
					public void write(byte[] b, int pos, int tam) {
						final String texto = new String(b, pos, tam, java.nio.charset.Charset.forName("UTF-8"));
						escrever(texto);
					}
					@Override
					public void write(int b) {
						write(new byte[]{(byte) b}, 0, 1);
					}
				}, false, "UTF-8");
			System.setOut(ps);
			System.setErr(ps);
		} catch(Exception e) {
			System.err.println("[ERRO]: "+e);
		}
    }

    public void escrever(final String texto) {
        ui.post(new Runnable() {
				@Override
				public void run() {
					saida.append(texto);
					scroll.post(new Runnable() {
							@Override
							public void run() {
								scroll.fullScroll(ScrollView.FOCUS_DOWN);
							}
						});
				}
			});
    }

    public void pedirPermissoes() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()) {
                Intent cache = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                cache.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(cache, 1);
            }
        } else {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }
	
	public static void introducao() {
		System.out.println("[Terminal Simples]");
		System.out.println("# digite: instalar listar");
		System.out.println("# para ver todos pacotes disponiveis online");
	}
}

