# Velum 0.1

Aplicativo Android de catálogo e reprodução. Esta versão mostra um catálogo de demonstração e reproduz arquivos oficiais da Blender Foundation. Não é um painel, não tem conta e não instala add-ons.

O APK de depuração foi compilado uma vez, com `assembleDebug`. Ele **não foi instalado num telefone**, então a imagem e o som ainda precisam ser confirmados no aparelho.

## O que dá para fazer

- Ver o início, buscar no catálogo local e abrir os detalhes.
- Assistir ao trailer de Sintel. Esse é o vídeo de teste.
- Trocar a fonte quando o título tiver mais de uma.
- Sair no meio e voltar perto do ponto em que parou.
- Ver o aviso de tamanho antes de Tears of Steel (cerca de 372 MB).

## No celular, agora

O arquivo para instalar fica neste endereço, depois que a Action ficar verde:

https://github.com/StartStatic1/velum/releases/download/v0.1.0/Velum.apk

1. Abra esse link no celular. O arquivo tem que se chamar `Velum.apk`.
2. Não extraia e não abra como pasta.
3. Se o Android bloquear, permita a instalação a partir do navegador.
4. Abra o Velum, toque em Sintel e depois em Assistir.
5. A internet precisa estar ligada. O vídeo vem de `download.blender.org`.

Isso não publica o aplicativo na Play Store. É uma instalação direta da versão de depuração.

## Três lugares, se for gerar de novo

### 1. O código

A pasta do projeto vai para um repositório seu no GitHub, com `settings.gradle.kts` na raiz.

Pelo celular:

1. Instale o Termux.
2. Copie o zip do código para o Termux e descompacte.
3. Entre na pasta descompactada.

```sh
pkg install git unzip
unzip velum-0.1.0-codigo.zip
cd velum
git init
git add .
git commit -m "Velum 0.1"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/velum.git
git push -u origin main
```

Troque `SEU_USUARIO`. Crie o repositório vazio no GitHub antes do push.

### 2. A compilação, no GitHub, não no celular

O arquivo `.github/workflows/android-debug.yml` compila numa máquina Ubuntu do GitHub.

No site do repositório, abra Actions. Se o fluxo não começar, use **Run workflow** em "APK de depuração".

Verde: os testes do catálogo passaram e um APK novo foi gerado. Vermelho: leia o log antes de instalar.

O teste do catálogo, sem Android SDK:

```sh
./gradlew :core:model:test -PmodelOnly=true
```

O APK:

```sh
./gradlew :core:model:test :app:assembleDebug
```

### 3. O aparelho

Quando a Action ficar verde, o mesmo APK também aparece em Releases, com o nome `Velum.apk`. É esse arquivo que se instala.

## Quando esta etapa está concluída

- O APK instala num telefone.
- Sintel começa a tocar.
- Sair e voltar continua perto do ponto anterior.
- Big Buck Bunny abre, e a troca de fonte não quebra a tela.
- Nenhum endereço fora de `download.blender.org` foi adicionado.

A compilação já aconteceu. A etapa só fecha depois do teste no aparelho. A fase seguinte não é add-on: é anotar o que quebrou nesse teste.
