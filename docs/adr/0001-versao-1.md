# ADR 0001 — Velum 0.1

Data da decisão: 16 de outubro de 2026.

## Nome e pacote

- Nome: Velum.
- `applicationId` e namespace do aplicativo: `app.velum`.
- Identidade: fundo `#0B0D10`, texto `#E7E4DC`, acento `#E8A54B`. Cartazes tipográficos, sem imagens de terceiros.
- O código, o nome e a interface são próprios. Não há código nem layout de outro aplicativo.

## O que entra na 0.1

- Telefone Android, Jetpack Compose, navegação entre início, detalhes e player.
- Catálogo local com três obras da Blender Foundation, licença Creative Commons Attribution 3.0.
- Reprodução com Media3 ExoPlayer. A fonte inicial de teste é o trailer oficial de Sintel, em MP4, servido por `https://download.blender.org`.
- Troca entre as fontes listadas no próprio título.
- Posição de reprodução guardada no DataStore do aparelho.
- Testes de JVM do catálogo, das URLs e da conta da posição.

## O que fica para depois

Não há módulo vazio para isto. Entra só quando a fase anterior estiver concluída e testada:

1. Metadados TMDB, com a chave apenas num servidor, nunca no APK.
2. Add-ons. Quando existirem, só HTTPS, sem rede privada e só domínios autorizados.
3. Painel administrativo e contas.
4. Android TV como interface própria.
5. Minify de release, depois de um APK de release ser compilado e instalado de verdade.

## Módulos

- `core/model`: dados e regras puras, sem Android.
- `core/designsystem`: tema e componentes.
- `feature/home`, `feature/details`, `feature/player`.
- `app`: activity e navegação.

## Dependências conferidas em 16 de outubro de 2026

| Peça | Versão | Fonte da conferência |
| --- | --- | --- |
| Android Gradle Plugin | 9.4.0 | Notas oficiais: Gradle 9.6.0, JDK 17, API máxima 37 |
| Kotlin | 2.4.10 | Página oficial de configuração do Compose |
| Compose BOM | 2026.09.00 | Mesma página; Compose 1.12 pede compileSdk 37 e AGP 9 |
| Navigation Compose | 2.10.2 | Notas do Jetpack Navigation |
| Media3 | 1.11.1 | Notas do Jetpack Media3 |
| DataStore | 1.2.1 | Notas do Jetpack DataStore |
| Activity Compose | 1.13.0 | Notas do Jetpack Activity |
| Lifecycle | 2.11.0 | Notas do Jetpack Lifecycle |
| Coroutines | 1.10.2 | Maven Central, artefato kotlinx-coroutines-core |
| Coil, Retrofit, Room, Supabase | fora da 0.1 | Não há imagem remota, API nem banco |

`compileSdk` 37. `targetSdk` 36. `minSdk` 26 (Android 8). Boxes mais antigas ficam de fora até alguém provar que a pilha ainda as suporta.

## Conteúdo

Os endereços foram consultados em 16 de outubro de 2026 e responderam HTTPS 200:

- Sintel trailer 720p, MP4, 7.608.204 bytes.
- Sintel trailer 480p, MP4, 4.372.373 bytes.
- Big Buck Bunny trailer, M4V, 3.889.885 bytes.
- Big Buck Bunny trailer 480p, MOV, 11.061.011 bytes.
- Tears of Steel 720p, MOV, 372.178.639 bytes.

O aplicativo não inclui esses arquivos. Se um endereço mudar, a tela mostra erro e a troca só deve acontecer depois de uma nova consulta.

## Compilação

Em 16 de outubro de 2026, `./gradlew :core:model:test` passou com 7 testes e `./gradlew :app:assembleDebug` gerou o APK de depuração. Não houve instalação em telefone nem reprodução confirmada na tela.

