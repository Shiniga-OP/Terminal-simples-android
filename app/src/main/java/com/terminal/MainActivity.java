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

        executor = new Executor(getFilesDir());

        entrada.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					boolean enter = actionId == EditorInfo.IME_ACTION_DONE ||
						(event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN);
					if(enter) {
						String cmd = entrada.getText().toString().trim();
						if(!cmd.isEmpty()) {
							escrever("$ " + cmd + "\n");
							entrada.setText("");
							executor.exec(cmd);
						}
						return true;
					}
					return false;
				}
			});
    }

    public void redirecionarSaida() {
		try {
			PrintStream ps = new PrintStream(new OutputStream() {
					@Override
					public void write(byte[] b, int off, int len) {
						final String texto = new String(b, off, len, java.nio.charset.Charset.forName("UTF-8"));
						escrever(texto);
					}
					@Override
					public void write(int b) {
						write(new byte[]{(byte) b}, 0, 1);
					}
				}, false, "UTF-8");
			System.setOut(ps);
			System.setErr(ps);
		} catch(Exception e) {}
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
}

