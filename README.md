## um terminal simples que usa comandos padrão do shell Android
## para instalar pacotes de binários.
você pode utilizar um .zip com o nome de "pacotes.zip" e utilize o comando:

instalar /caminho/absoluto/do/pacote.zip

e espere até a mensagem de sucesso aparecer.

ou você pode simplesmente levar as pastas com as dependências necessárias para o app, na pasta utilizando o comando.

cp /caminho/do/binario.bin /data/data/com.terminal/files/pacotes/bin/

e então executar normalmente, talvez você precise colocar.

chmod +x binario

para que ele funcione.

## para dependências.

você pode colocar as bibliotecas necessárias em:

/data/data/com.terminal/files/pacotes/libs/

e caso precise de include em binários C:

/data/data/com.terminal/files/pacotes/include/

em casos como NodeJS, que necessita de openssl.cnf, você pode deixar essas dependências em:

/data/data/com.terminal/files/pacotes/etc/

## comandos especiais:

para limpar os logs na tela, digite:

limp

para instalar um pacote oficial já zipado, digite:

instalar node

ou

instalar asm

isso instalará o pacote sem a necessidade de trabalho manual.

## informações extras:

caso um comando com quebra de linha "\n" seja utilizado, será tratado como 2, então não execute comandos sem que o cursor esteja no final do comando.

o terminal não utiliza de bibliotecas externas para funcionar padrão, por tanto pode ser compilado diretamente com AIDE, Sketchware, ou CodeAssistent no Android.

feito para compatibilidade com Androids anteriores, API usada somente até Java 7.
