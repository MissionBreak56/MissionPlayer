# MissionPlayer

Player de música **local, offline e sem anúncios** para Android. Lê as músicas que já estão no
aparelho e oferece uma interface moderna, equalizador, letras sincronizadas e muita
personalização.

> Projeto original: nenhum código, recurso, texto, ícone ou marca de outro app foi copiado.
> Pacote: `com.missionbreak56.musicplayer` · Repositório: https://github.com/MissionBreak56/MissionPlayer

[![Último release](https://img.shields.io/github/v/release/MissionBreak56/MissionPlayer)](https://github.com/MissionBreak56/MissionPlayer/releases/latest)

## Instalação

1. Baixe o APK mais recente em **[Releases](https://github.com/MissionBreak56/MissionPlayer/releases/latest)**
   (`MissionPlayer-vX.Y.Z-arm64-v8a.apk`).
2. Abra o arquivo no celular e permita a instalação de fontes desconhecidas, se o Android pedir.
3. Na primeira abertura, permita o acesso às músicas (e às notificações, para os controles de
   reprodução).

**Requisitos:** Android 6.0 (API 23) ou superior, processador ARM 64 bits (arm64-v8a).

> **Atualizando da v2.4.1 ou anterior?** Desinstale a versão antiga antes: até a v2.4.1 cada APK era
> assinado com uma chave diferente. Da **v2.4.2 em diante** todas as versões usam a mesma chave e
> atualizam por cima, sem perder dados.

## Funções

### Biblioteca
- **Abas:** Músicas, Álbuns, Artistas e Pastas, que você pode **reorganizar** (Configurações → *Organizar
  abas*); a ordem fica salva.
- **Atalhos na tela inicial:** Favoritos, Playlists e Recente.
- **Músicas:** botão de reprodução aleatória e ordenação (botão de ordenar, a opção atual vem marcada)
  por **A a Z, Z a A, Mais recentes, Mais tocadas, Artista, Álbum ou Duração**. A ordem alfabética
  ignora acentos (Á fica junto do A).
- **Barra alfabética (# A–Z)** na lateral direita das listas: toque ou arraste para pular direto para
  uma letra, com a letra em destaque no centro da tela. Na lista de Músicas ela só aparece na ordem
  A a Z. **Dentro de álbuns, pastas, artistas e listas** a barra também aparece (em listas com mais
  de 12 músicas) quando a ordem é A a Z, Artista ou Álbum.
- **Ordenar dentro de álbuns, pastas, artistas, gêneros e playlists:** botão de ordenar no topo da
  tela, com *Ordem padrão* (a própria do álbum ou da lista) e as mesmas opções da lista de Músicas.
  A escolha fica salva para cada tipo de lista, e *Tocar tudo* e *Aleatório* seguem a ordem escolhida.
- **Álbuns** em grade de 3 colunas, com capas.
- **Pastas:** navegue pelas pastas do aparelho; cada pasta mostra sua foto no cabeçalho.
- **Ocultar** músicas e pastas (só a pasta escolhida, as subpastas continuam visíveis). Para desfazer:
  Configurações → *Itens ocultos*.

### Busca
- Procura por título, artista, álbum e também **dentro das letras**: lê a letra gravada no arquivo
  (MP3/ID3, FLAC, OGG, M4A), arquivos `.lrc`/`.txt` com o mesmo nome da música e letras adicionadas
  no app. O trecho encontrado aparece abaixo do título.

### Playlists
- **Automáticas:** Favoritas, Adicionadas recentemente, Mais tocadas e Tocadas recentemente.
- **Suas playlists:** crie, renomeie e apague; adicione músicas pelo menu ⋮ da música.

### Menu da música (⋮)
Tocar a seguir, adicionar à fila, adicionar à playlist, favoritar, definir como toque, compartilhar
e ocultar.

### Foto da pasta
Em *Pastas*, menu ⋮ da pasta (ou ⋮ dentro da pasta):
- **Alterar foto da pasta** — escolhe uma imagem da galeria;
- **Escolher foto das músicas** — mostra as capas das músicas daquela pasta para você tocar numa;
- **Remover foto da pasta**.

A foto aparece apenas dentro da pasta (cabeçalho do detalhe), não na lista.

### Mini player (liquid glass)
- Aparece em todas as telas de lista, com capa circular, título, artista, progresso, play/pause,
  próxima e fila.
- O fundo **mistura as cores da capa** da música em um gradiente suave, com brilho de vidro no topo e
  borda translúcida, e muda com uma transição animada a cada faixa.
- Texto, ícones, botão de play e barra de progresso ajustam o contraste (claro/escuro)
  automaticamente conforme as cores da capa. Sem capa, usa as cores do tema escolhido.

### Tela Tocando agora
- **Liquid glass em tela cheia:** o fundo mistura as cores da capa com manchas de luz translúcidas e
  brilho de vidro, com transição suave ao trocar de música; título, artista, tempo, barra de
  progresso, ícones e letras ficam brancos para ler bem em qualquer capa. Sem capa, usa as cores do
  tema.
- Capa grande, barra de progresso, aleatório, repetir (desligado / tudo / uma), favorito, fila, letra
  e temas. Os botões de voltar e avançar são maiores; o repetir fica ao lado do avançar e a fila na
  linha de baixo, à esquerda.
- **Deslize a capa** para a esquerda para ir à próxima música e para a direita para voltar à anterior;
  tocar na capa abre a letra.
- **Letras:** anexe um arquivo `.lrc` (sincronizado — toque numa linha para pular até ela) ou `.txt`
  a cada música; se não houver, usa a letra embutida no arquivo ou o `.lrc` vizinho.

### Aleatório e repetição
- **Aleatório sem repetir:** todas as músicas da fila tocam uma vez, começando pela atual, antes de
  qualquer uma repetir. Vale ao tocar uma lista, ao ligar o aleatório com música tocando e para
  músicas adicionadas à fila (*Tocar a seguir* entra logo depois da atual; *Adicionar à fila* entra
  entre as que ainda não tocaram).
- **Aleatório que nunca termina:** ao fim da rodada, o botão avançar continua funcionando e a
  reprodução segue sozinha: o app escolhe uma música diferente (a que tocou há mais tempo) para abrir
  uma nova rodada embaralhada, sem repetir a que acabou de tocar. Com o aleatório ligado, *repetir
  desligado* se comporta como *repetir todas*.
- **Modos salvos:** a última escolha de repetir e de aleatório volta como você deixou.

### Retomar de onde parou
O app guarda a fila, a música atual e o ponto exato (atualizado a cada 5 s tocando, ao pausar, ao
trocar de música e ao fechar). Ao reabrir, o mini player mostra a última música pausada no mesmo
ponto. Músicas apagadas ou ocultas desde então são ignoradas. Se o Android parar o serviço de música
(por exemplo com a música pausada), o player recoloca a última fila ao voltar ao app, em vez de
fechar.

### Configurações
Toque na engrenagem no topo da tela inicial. Tudo o que ficava no menu flutuante agora está aqui:
- **Aparência:** *Temas* (tema do app e modo claro/escuro/do sistema) e *Personalização avançada*.
- **Biblioteca:** *Organizar abas* e *Itens ocultos*.
- **Áudio e letras:** *Equalizador* e a chave de *Letras automáticas*.
- **Mais:** *Estatísticas* e *Sobre*.

### Personalização avançada
Configurações → *Personalização avançada*:
- **Ícone do app:** 9 opções — *Original*, e variações *Verde, Azul, Violeta, Dourado, Rosa*, mais
  *Vinil, Ondas* e *Fones*. A troca é imediata (a tela inicial pode levar alguns segundos para
  atualizar). Os ícones também têm versão monocromática para o Android 13+ (ícones temáticos).
- **Cor de destaque:** 16 cores prontas, *Usar a cor do tema* ou a **roda de cores**: arraste no
  círculo para escolher qualquer tom (passos de 5°) e toque numa das 5 intensidades (do pastel ao
  escuro). Cada escolha tem versão clara e escura.
- **Cor do fundo:** *Padrão, Puro, Grafite, Azul-noite, Sépia* e *Musgo*, cada um com versão clara e
  escura. Também dá para usar uma **imagem da galeria** como fundo, com controle de visibilidade
  (a imagem se mistura com a cor do fundo para o texto continuar legível) e botão para remover. A
  tela *Tocando agora* mantém o visual liquid glass da capa.
- **Fonte:** *Usar a fonte do tema* (cada tema completo tem a sua) ou uma de 19 fontes: *Serifada,
  Monoespaçada, Condensada, Leve, Média, Casual, Cursiva, Pesada, Padrão do sistema, Fina, Versalete,
  Condensada leve, Condensada média, Serifa mono* e as embutidas *Arredondada (Fredoka), Manuscrita
  (Amatic), Rabisco (Intuitive), Serifa elegante (Source Serif)* e *Moderna (Hanken Grotesk)*. A
  fonte escolhida vale em todo o app, inclusive com os temas completos.
- **Cantos:** *Padrão, Retos, Suaves* ou *Redondos* para botões, cartões e janelas.
- *Restaurar padrões* volta tudo (inclusive o ícone e a imagem de fundo) ao original.

As escolhas valem por cima do tema escolhido em *Temas* e ficam salvas. As fontes embutidas usam a
SIL Open Font License (texto em `FONTS-OFL.txt`).

### Estatísticas
Menu ⋮ → *Estatísticas*: total de músicas, reproduções, tempo ouvido, tempo da biblioteca, favoritas
e álbuns, além das **músicas, artistas e álbuns mais ouvidos**.

### Letras automáticas
- O app procura sozinho, em segundo plano e só quando há internet, as letras das músicas que não têm,
  consultando em ordem **LRCLIB**, **NetEase**, **Kugou** e **lyrics.ovh**. A primeira letra
  sincronizada vence; texto simples só entra se nenhuma fonte tiver a versão sincronizada.
- As letras baixadas ficam salvas no aparelho e funcionam **offline**, inclusive na busca por letra.
  Ao abrir uma música sem letra, ela é buscada na hora.
- A busca roda a cada 15 minutos (o menor intervalo que o Android permite), com 4 músicas ao mesmo
  tempo; músicas não encontradas são tentadas de novo após 15 minutos.
- Nada para configurar. Para desligar: Configurações → *Letras automáticas*.

### Equalizador e volume
- Equalizador do aparelho com presets e controles por banda, reforço de graves e surround.
- **Aumentar volume** além do máximo normal, até cerca de +15 dB (funciona sem ligar o equalizador).
  Valores altos podem distorcer o som e prejudicar a audição ou o alto-falante.

### Aparência
- 8 cores de destaque e modo **claro / escuro / do sistema** (fundo preto no modo escuro).
- **7 temas completos** com visual próprio (cores, fonte e formato dos cantos): *Neon, Papel, Oceano,
  Pôr do sol, Mono, Floresta* e *Rosé*. Escolha em Configurações → *Temas*.
- **Tela cheia:** o app ocupa a tela toda, inclusive atrás do entalhe/câmera e da barra de status, sem
  faixa preta no topo; o conteúdo continua longe da câmera e das barras.
- Ícone adaptativo (Android 8+), com versão monocromática para o tema de ícones do Android 13+.
- Idiomas: **português (Brasil)** e **inglês**.

### Reprodução em segundo plano
Serviço de mídia (Media3): continua tocando com a tela desligada, com notificação e controles na
tela de bloqueio.

### Privacidade
Sem anúncios, sem rastreadores e sem contas. A internet é usada **só** para baixar letras que faltam
(título, artista, álbum e duração são enviados aos serviços de letras acima); a reprodução é 100%
local. Desligue em Configurações → *Letras automáticas* para o app não usar a rede.

## Permissões

| Permissão | Para quê |
|---|---|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` (até Android 12) | Ler as músicas do aparelho |
| `FOREGROUND_SERVICE` (+ `_MEDIA_PLAYBACK`) e `WAKE_LOCK` | Tocar em segundo plano |
| `INTERNET` e `ACCESS_NETWORK_STATE` | Baixar letras que faltam e rodar a busca só quando há internet |
| `POST_NOTIFICATIONS` | Notificação com os controles de reprodução |
| `MODIFY_AUDIO_SETTINGS` | Equalizador, graves, surround e aumento de volume |
| `WRITE_SETTINGS` | Somente se você usar *Definir como toque* |

## Tecnologia

Kotlin · AndroidX · Material Components 1.12 · Media3 (ExoPlayer, MediaSession) 1.4.1 · Palette ·
View Binding · `minSdk 23` · `targetSdk 34` · `compileSdk 34`.

## Estrutura do projeto

```
app/src/main/java/com/missionbreak56/musicplayer/
├── App.kt
├── data/      Song, MusicRepository (MediaStore), Library (álbuns, artistas, pastas, playlists),
│              Prefs (favoritos, playlists, estatísticas, tema, equalizador, modos e fila salvos),
│              Lyrics (leitor .lrc e índice de busca), LyricsFetcher (fontes de letra online),
│              LyricsWorker (busca automática em segundo plano), Labels
├── playback/  PlaybackService (sessão, ordem aleatória justa, salvamento de estado), PlaybackQueue,
│              PlaybackState (salvar/restaurar fila), PlayerConnection, AudioFx (equalizador e aumento
│              de volume), SleepTimer (código mantido, sem acesso na interface)
└── ui/        MainActivity (abas), DetailActivity, SearchActivity, PlaylistsActivity, PlayerActivity,
               EqualizerActivity, ThemesActivity, SettingsActivity (configurações), CustomizeActivity
               (personalização avançada), ColorWheelView (roda de cores), BgImage (imagem de fundo),
               IconManager (troca de ícone), StatsActivity, AboutActivity, AlphaIndexBar (barra # A–Z),
               FolderArt / FolderArtPicker (foto da pasta), BaseActivity (tela cheia), adaptadores e diálogos
```

## Histórico de versões

| Versão | Principais mudanças |
|---|---|
| **2.9.0** | Tela de Configurações (substitui o menu flutuante); roda de cores no destaque; imagem da galeria como fundo; 19 fontes e *Usar a fonte do tema*; corrige a fonte que não mudava com os temas completos |
| **2.8.0** | Barra alfabética e ordenação dentro de álbuns/pastas/artistas/listas; personalização avançada (9 ícones, 16 cores de destaque, 6 fundos, 9 fontes, 4 estilos de cantos) |
| **2.7.0** | Letras automáticas (LRCLIB, NetEase, Kugou, lyrics.ovh), estatísticas e 7 novos temas |
| **2.6.7** | Listas aparecem bem mais rápido: cópia salva da biblioteca, ordenação mais rápida e aviso "Carregando…" |
| **2.6.6** | README completo e texto da tela *Sobre* atualizados |
| **2.6.5** | Tela cheia (sob o entalhe); o player não fecha mais com a fila vazia; ícone de lista só com linhas |
| **2.6.4** | Repetir e Fila trocam de lugar; novo ícone de letras |
| **2.6.3** | Aleatório contínuo: nova rodada com música diferente ao fim da lista |
| **2.6.2** | Corrige o toque ignorado ao voltar de outra tela |
| **2.6.1** | Liquid glass também na tela Tocando agora |
| **2.6.0** | Barra alfabética (# A–Z), mais opções de ordenação e botões maiores no player |
| **2.5.1** | Mini player liquid glass com as cores da capa |
| **2.5.0** | Salva a última música, aleatório sem repetir e salva repetir/aleatório |
| **2.4.2** | Chave de assinatura fixa para todas as versões |
| **2.4.1** | Deslizar a capa no player para trocar de música |
| **2.4.0** | Foto da pasta a partir das músicas; aumento de volume além do padrão |
| **2.3.2** | Corrige o ícone do app, que não carregava |
| **2.3.1** | Novo ícone; corrige botão *Aleatório* e a barra de progresso do player |
| **2.3.0** | Abas reorganizáveis; tela de Playlists própria; remove abas Playlists e Gêneros |
| **2.2.0** | Foto personalizada da pasta; menus arredondados; subpastas visíveis; remove o temporizador; link do GitHub no Sobre |
| **2.1.0** | Nova interface; busca dentro das letras; ocultar músicas e pastas; álbuns menores; corrige a lista em 2 colunas e as capas que não carregavam |
| **2.0.0** | Novo visual e publicação automática de release |

Os detalhes de cada versão estão na página de [Releases](https://github.com/MissionBreak56/MissionPlayer/releases).
