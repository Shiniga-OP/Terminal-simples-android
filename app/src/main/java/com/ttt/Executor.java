package com.ttt;

import java.util.Map;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.HashMap;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.io.FileOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import android.os.Build;

public class Executor {
	public File dirTrabalho, dirPs, dirTmp, dirExec;
    public List<String> bins = new ArrayList<>();
	public Map<String, String> pacotes = new HashMap<>();
	public ProcessBuilder pb;

	public Process sh;
	public OutputStream stdin;
	public BufferedReader stdout;
	public static final String SENTINEL = "__FIM__";

	public Executor(File ambiente) {
		// configurando diretorios:
        dirTrabalho = new File(ambiente.getAbsolutePath(), "CASA");
        if(!dirTrabalho.isDirectory()) dirTrabalho.mkdirs();
        dirPs = new File(ambiente.getAbsolutePath(), "pacotes");
		if(!dirPs.isDirectory()) dirPs.mkdir();
		dirTmp = new File(dirPs.getAbsolutePath(), "tmp");
		if(!dirTmp.isDirectory()) dirTmp.mkdir();

		pb = new ProcessBuilder();
		pb.directory(dirTrabalho);

		// adicionando as pastas:
		addBin("include");
        addBin("bin");
        addBin("lib");

		// configurando pacotes:
        pacotes.put("node", "https://github.com/Shiniga-OP/Terminal-simples-android/releases/download/NodeJS-v22.17.1-arm64/node.zip");
        pacotes.put("asm", "https://github.com/Shiniga-OP/Terminal-simples-android/releases/download/Assembly-arm64/asm.zip");
        pacotes.put("clang", "https://github.com/Shiniga-OP/Terminal-simples-android/releases/download/Clang-20.1.8-arm64/clang.zip");

		iniciarSh();
	}

	public void iniciarSh() {
		try {
			configAmbiente(pb);
			pb.command("/system/bin/sh");
			pb.redirectErrorStream(true);
			sh = pb.start();
			stdin = sh.getOutputStream();
			stdout = new BufferedReader(new InputStreamReader(sh.getInputStream(), "UTF-8"));
			defPermissoes();
		} catch(Exception e) {
			System.err.println("Erro ao iniciar shell: " + e.getMessage());
		}
	}
	
	public void defPermissoes() {
		File binDir = new File(dirPs, "bin");
		if(binDir.exists() && binDir.isDirectory()) {
			for(File bin : binDir.listFiles()) {
				if(bin.isFile()) {
					bin.setExecutable(true);
				}
			}
		}
	}

	public void exec(final String comandoStr) {
        new Thread(new Runnable() {
				@Override
				public void run() {
					if(comandoStr.startsWith("instalar ")) {
						String c = comandoStr.substring(9).trim();
						if(pacotes.containsKey(c)) instalarWeb(pacotes.get(c));         
						else {
							if((new File(c).exists())) instalarPacote(c);
							else System.out.println("este arquivo não existe");
						}
						return;
					}
					execProcesso(comandoStr);
				}
			}).start();
    }

	public void instalarWeb(final String url) {
        System.out.println("baixando pacote...");

        final File zip = new File(dirTmp, "pacote.zip");
        try {
            URL u = new URL(url);
            InputStream en = u.openStream();
            FileOutputStream s = new FileOutputStream(zip);
            byte[] buffer = new byte[8192];
            int l;
            while((l = en.read(buffer)) > 0) s.write(buffer, 0, l);
            s.close();
            en.close();
            instalarPacote(zip.getAbsolutePath());
        } catch(Exception e) {
            System.err.println("Falha no download: " + e.getMessage());
        }
    }

	public void instalarPacote(String cam) {
		System.out.println("instalando pacote...");

        final File zipArq = new File(cam);
        if(!zipArq.exists()) return;
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipArq));
            ZipEntry entradas;
            File destDir = new File(dirPs.getAbsolutePath());
            if(!destDir.exists() && !destDir.mkdirs()) {
                System.err.println("Falha ao criar diretório pacotes");
                return;
            }
            byte[] buffer = new byte[8192];
            while((entradas = zis.getNextEntry()) != null) {
                if(entradas.isDirectory()) {
					zis.closeEntry();
					continue;
				}
                File saidaArq = new File(destDir, entradas.getName());
                File parente = saidaArq.getParentFile();
                if(!parente.exists() && !parente.mkdirs()) {
                    System.err.println("Falha ao criar diretório " + parente.getAbsolutePath());
                    zis.closeEntry();
                    continue;
                }
                FileOutputStream fos = new FileOutputStream(saidaArq);
                int tam;
                while((tam = zis.read(buffer)) > 0) fos.write(buffer, 0, tam);
				
                fos.getFD().sync();
                fos.close();
                zis.closeEntry();
            }
            zis.close();
            System.out.println("[PACOTE INSTALADO]");
            if(cam.startsWith(dirTmp.getAbsolutePath())) zipArq.delete();
        } catch(Exception e) {
            System.err.println("Erro ao instalar binários: " + e.getMessage());
        }
		execProcesso("chmod +x "+dirPs.getAbsolutePath()+"/bin/*");
    }  

	public synchronized void execProcesso(String comando) {
        try {
            final String comandoFinal = enrolarLinker(comando.trim());
            // envia o comando + sentinel com codigo de saída
            stdin.write((comandoFinal + "\necho " + SENTINEL + "$?\n").getBytes());
            stdin.flush();

            final StringBuilder resultado = new StringBuilder();
            String linha;
            while((linha = stdout.readLine()) != null) {
                if(linha.startsWith(SENTINEL)) {
                    System.out.println(resultado.toString());
                    System.out.println(linha.substring(SENTINEL.length()));
                    break;
                }
                resultado.append(linha).append("\n");
            }
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }

	public void configAmbiente(ProcessBuilder pb) {
        Map<String, String> cams = pb.environment();

        String camAtual = cams.get("PATH");
        if(camAtual == null) camAtual = "";
        if(bins != null) {
            for(String bin : bins) {
                if(bin == null) continue;
                camAtual = bin + ":" + camAtual;
            }
        }
        cams.put("PATH", camAtual);

        String sysrootBiblis = dirPs.getAbsolutePath() + "/lib";
        String localInclude = dirPs.getAbsolutePath() + "/include";
        String clangInclude = dirPs.getAbsolutePath() + "/usr/clang/20/include";
        String usrInclude = dirPs.getAbsolutePath() + "/usr/include";

		// clang
        addVar(cams, "C_INCLUDE_PATH", localInclude + ":" + clangInclude + ":" + usrInclude);
        addVar(cams, "CPATH", localInclude + ":" + clangInclude + ":" + usrInclude);
        addVar(cams, "CPLUS_INCLUDE_PATH", localInclude + ":" + clangInclude + ":" + usrInclude);
		
		// LD:
        String bibliDir = sysrootBiblis + "/usr/lib";
        addVar(cams, "LIBRARY_PATH", bibliDir + ":" + sysrootBiblis);
        addVar(cams, "LD_LIBRARY_PATH", bibliDir + ":" + sysrootBiblis);
		
		// geral:
        cams.put("TMPDIR", dirTmp.getAbsolutePath());
        cams.put("SYSROOT", sysrootBiblis);
        cams.put("PACOTES_DIR", dirPs.getAbsolutePath());
        addVar(cams, "OPENSSL_CONF", dirPs.getAbsolutePath() + "/etc");

        // node
        cams.put("HOME", dirTrabalho.getAbsolutePath());
        cams.put("NODE_PATH", dirPs.getAbsolutePath() + "/lib/node_modules");
        cams.put("NODE_REPL_HISTORY", dirTmp.getAbsolutePath() + "/.node_repl_history");
        cams.put("XDG_DATA_HOME", dirPs.getAbsolutePath());
    }

	public String enrolarLinker(String comando) {
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return comando; // so android 10+ usa linker64

        String[] partes = comando.trim().split("\\s+", 2);
        String nomeBin = partes[0];
		
        String args = partes.length > 1 ? " " + partes[1] : "";
        File binEncontrado = null;
        for(String dir : bins) {
            File c = new File(dir, nomeBin);
			
            if(c.exists() && c.isFile()) {
				binEncontrado = c;
				break;
			}
        }
        if(binEncontrado == null) return comando;
        try {
            FileInputStream fis = new FileInputStream(binEncontrado);
            byte[] h = new byte[18];
            int n = fis.read(h);
            fis.close();
            if(n >= 18 && h[0]==0x7f && h[1]=='E' && h[2]=='L' && h[3]=='F'
			   && h[16]==3 && h[17]==0) {
				if(n >= 18 && h[0]==0x7f && h[1]=='E' && h[2]=='L' && h[3]=='F') {
					boolean eh64 = (h[4] == 2); // ELFCLASS64
					if(eh64) {
						int e_tipo = ((h[16] & 0xFF) | ((h[17] & 0xFF) << 8));
						if(e_tipo == 3) { // ET_DYN = PIE
							return comando;
						}
					}
					return "/system/bin/linker64 " + binEncontrado.getAbsolutePath() + args;
				}
            }
        } catch(Exception e) {}
        return comando;
    }

    public static void addVar(Map<String, String> cams, String chave, String prefixo) {
        String atual = cams.get(chave);
        if(atual == null) atual = "";
        cams.put(chave, prefixo + ":" + atual);
    }

	public void addBin(String var) {
		bins.add(dirPs.getAbsolutePath()+File.separator+var);
	}
}

