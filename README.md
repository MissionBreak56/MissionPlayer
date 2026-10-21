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
- **Pastas:** navegue pelas pastas do aparelho; cada pasta mostra sua foto no cabeçalho. Os ícones de pasta aparecem grandes, sem fundo atrás.
- **Ocultar** músicas e pastas (só a pasta escolhida, as subpastas continuam visíveis). Para desfazer:
  Configurações → *Itens ocultos*.

### Busca
- Procura por título, artista, álbum e também **dentro das letras**: lê a letra gravada no arquivo
  (MP3/ID3, FLAC, OGG, M4A), arquivos `.lrc`/`.txt` com o mesmo nome da música e letras adicionadas
  no app. O trecho encontrado aparece abaixo do título.

### Playlists
- **Automáticas:** Favoritas, Adicionadas recentemente, Mais tocadas e Tocadas recentemente.
- **Suas playlists:** crie, renomeie e apague; adicione músicas pelo menu ⋮ da música.
- **Adicionar músicas dentro da playlist:** abra a playlist → menu ⋮ → **Adicionar músicas**. Abre uma
  lista com todas as músicas que ainda não estão nela, com busca (ignora acentos) e caixinhas para marcar
  várias de uma vez; o botão **Adicionar (N)** envia todas para a playlist.
- **Enviar músicas de uma pasta para uma playlist:** veja *Selecionar músicas na pasta* abaixo.
- **Foto da playlist:** veja *Foto da playlist* abaixo.

### Foto da playlist
Na tela *Playlists* (⋮ de cada playlist) ou dentro da playlist (⋮ no topo):
- **Alterar foto da playlist** — escolhe uma imagem da galeria;
- **Escolher foto das músicas** — mostra as capas das músicas da playlist para você tocar numa;
- **Remover foto da playlist** — volta para a capa da primeira música.

Diferente da foto da pasta, a foto da playlist **substitui a mini capa** que aparece na lista de
playlists e também aparece no cabeçalho da playlist. Ao renomear a playlist a foto é mantida; ao apagá-la,
a foto é removida. As playlists automáticas (Favoritas, Mais tocadas...) não têm foto personalizada.

### Selecionar músicas na pasta
Dentro de uma pasta, **segure o dedo numa música** ou use o menu ⋮ → **Selecionar músicas**. Cada música
ganha uma caixinha; toque para marcar/desmarcar, use **Todas** / **Nenhuma** e o botão de playlist (ícone
de lista com +) para escolher uma playlist existente ou criar uma nova. Músicas que já estão na playlist
não são duplicadas, e o aviso mostra quantas foram adicionadas. O botão voltar (ou o X) sai da seleção.

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
- **Capa girando estilo vinil:** enquanto a música toca, a capa circular gira bem devagar (uma volta a
  cada 20 s); ao pausar ela para no ângulo atual e, ao tocar de novo, continua de onde parou.
- O fundo **mistura as cores da capa** da música em um gradiente suave, com brilho de vidro no topo e
  borda translúcida, e muda com uma transição animada a cada faixa.
- Texto, ícones, botão de play e barra de progresso ajustam o contraste (claro/escuro)
  automaticamente conforme as cores da capa. Sem capa, usa as cores do tema escolhido.

### Tela Tocando agora
- **Liquid glass em tela cheia:** o fundo mistura as cores da capa com manchas de luz translúcidas e
  brilho de vidro, com transição suave ao trocar de música; título, artista, tempo, barra de
  progresso, ícones e letras ficam brancos para ler bem em qualquer capa. Sem capa, usa as cores do
  tema.
- **Temas do player** (botão de camisa no topo do player; só existe nele). A escolha fica salva:
  *Liquid glass* (padrão, mistura as cores da capa), *Preto AMOLED* (preto puro), *Capa desfocada*
  (a própria capa borrada ao fundo), *Aurora* (degradê diagonal mais vivo com as cores da capa),
  *Cor sólida* (cor principal da capa) e *Grafite* (cinza escuro fixo).
- **Abre de baixo:** ao tocar no mini player, a tela sobe de baixo para cima e desce ao fechar.
- Capa grande, barra de progresso, aleatório, repetir (desligado / tudo / uma), favorito, fila e letra. Os botões de voltar e avançar são maiores; o repetir fica ao lado do avançar e a fila na
  linha de baixo, à esquerda.
- **Deslize a capa** para a esquerda para ir à próxima música e para a direita para voltar à anterior;
  tocar na capa abre a letra.
- **Menu ⋮ do player:** definir como toque, compartilhar, remover letra, *Importar capa*, *Buscar capa
  online* e *Remover capa personalizada* (veja *Capas personalizadas*).
- **Letras:** anexe um arquivo `.lrc` (sincronizado — toque numa linha para pular até ela) ou `.txt`
  a cada música; se não houver, usa a letra embutida no arquivo ou o `.lrc` vizinho.

### Capas personalizadas
No menu ⋮ do player:
- **Importar capa** — usa uma imagem da galeria como capa daquela música.
- **Buscar capa online** — pesquisa capas pelo **nome da música** (iTunes Search, sem conta); o campo
  vem preenchido com o título e pode ser editado. Toque numa capa para aplicar.
- **Remover capa personalizada** — volta à capa original.

A capa escolhida vale para a música em todo o app (listas, mini player e player), as cores do liquid
glass acompanham a nova capa, a capa também aparece na **notificação de reprodução** e na tela de bloqueio, e ela fica salva só no aparelho.

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

### Reorganizar a fila
Com o **aleatório desligado**, abra a fila (botão de fila no mini player ou na tela Tocando agora) e
arraste uma música pelo ícone **≡** (ou segure o dedo nela) para mudar a ordem; a música que está
tocando continua tocando e a nova ordem vale para as próximas, sendo salva junto com a fila. Com o
aleatório ligado a ordem é sorteada, então a fila mostra o aviso *Desligue o aleatório para
reorganizar a fila*.

### Backup e restauração
Em Configurações → **Backup**:
- **Fazer backup:** abre o seletor de arquivos do Android, onde você escolhe a **pasta** e o nome do
  arquivo (por exemplo `MissionPlayer-backup-2026-10-20.zip`). Pode ser a memória interna, o cartão SD
  ou uma pasta sincronizada com a nuvem.
- **Restaurar backup:** escolha o arquivo e confirme. O app substitui os dados atuais pelos do arquivo
  e fecha para aplicar tudo; é só abrir de novo.

O arquivo guarda: favoritas, playlists (e suas fotos), músicas e pastas ocultas, estatísticas
(reproduções, tempo ouvido, recentes), ajustes de aparência (tema claro/escuro, cor, fonte, cantos,
fundo e imagem de fundo, ícone e nome do app), ordem das abas, ordenações, equalizador, aleatório/repetir,
letras automáticas, fotos das pastas, capas personalizadas de músicas e letras anexadas. A fila atual e
o ponto de reprodução não entram.

As músicas são salvas com caminho, título, artista e duração, porque o número interno do Android muda
em outro aparelho. Ao restaurar, cada música é encontrada de novo pelo caminho ou por título + artista +
duração; as que não existirem mais no aparelho ficam de fora e o app avisa quantas foram. O arquivo é
um `.zip` comum (com um `backup.json` legível) e fica só onde você o guardou.

### Retomar de onde parou
O app guarda a fila, a música atual e o ponto exato (atualizado a cada 5 s tocando, ao pausar, ao
trocar de música e ao fechar). Ao reabrir, o mini player mostra a última música pausada no mesmo
ponto. Músicas apagadas ou ocultas desde então são ignoradas. Se o Android parar o serviço de música
(por exemplo com a música pausada), o player recoloca a última fila ao voltar ao app, em vez de
fechar.

### Configurações
Toque na engrenagem no topo da tela inicial. Tudo o que ficava no menu flutuante agora está aqui:
- **Aparência:** *Personalização* (modo claro/escuro/do sistema, ícone e nome do app, cor de destaque, fundo e fonte).
- **Biblioteca:** *Organizar abas* e *Itens ocultos*.
- **Áudio e letras:** *Equalizador* e a chave de *Letras automáticas*.
- **Backup:** *Fazer backup* e *Restaurar backup*.
- **Mais:** *Estatísticas* e *Sobre*.

### Personalização
Configurações → *Personalização*. A tela abre com o visual padrão: **fundo preto puro** (no modo escuro),
**fonte do sistema** e uma **cor de destaque azul-escuro**.
- **Modo:** menu flutuante com *Do sistema*, *Claro* e *Escuro*.
- **Ícone do app:** menu flutuante com a imagem de cada ícone, 9 opções — *Original*, e variações *Verde, Azul, Violeta, Dourado, Rosa*, mais
  *Vinil, Ondas* e *Fones*. A troca é imediata (a tela inicial pode levar alguns segundos para
  atualizar). Os ícones também têm versão monocromática para o Android 13+ (ícones temáticos).
- **Nome do app:** campo para escolher o nome do app, com os botões *Aplicar nome* e *Nome original*. O
  nome aparece dentro do app (título da tela *Sobre*). O Android **não permite** que um app troque o
  próprio nome na gaveta de apps; por isso há o botão *Criar atalho na tela inicial com este nome*, que
  adiciona à tela inicial um atalho com o nome e o ícone atuais (o launcher pede a confirmação). Depois
  de criado, esse atalho acompanha as trocas de nome e de ícone. O nome original continua na gaveta de
  apps. Essa opção existe **somente** na tela de Personalização.
- **Cor de Destaque:** *Cor Padrão* (o azul-escuro fixo do app) ou uma cor personalizada: uma barra em arco-íris
  escolhe o tom (passos de 5°) e outra a intensidade (do pastel ao escuro), com prévia da cor. Cada
  escolha tem versão clara e escura. Quem usava uma das 16 cores prontas das versões anteriores
  mantém a cor, que passa a ser uma cor personalizada.
- **Cor do fundo:** *Puro* (padrão), *Grafite, Azul-noite, Sépia* e *Musgo*, cada um com versão clara e
  escura. Também dá para usar uma **imagem da galeria** como fundo, com controle de visibilidade
  (a imagem se mistura com a cor do fundo para o texto continuar legível) e botão para remover. A
  tela *Tocando agora* mantém o visual liquid glass da capa.
- **Fonte:** menu flutuante (cada nome aparece na própria fonte); *Padrão do sistema* fica no topo da lista e é a fonte padrão. Há 19 opções: *Serifada,
  Monoespaçada, Condensada, Leve, Média, Casual, Cursiva, Pesada, Padrão do sistema, Fina, Versalete,
  Condensada leve, Condensada média, Serifa mono* e as embutidas *Arredondada (Fredoka), Manuscrita
  (Amatic), Rabisco (Intuitive), Serifa elegante (Source Serif)* e *Moderna (Hanken Grotesk)*. A
  fonte escolhida vale em todo o app.
- *Restaurar padrões* pede confirmação e, se você confirmar, volta tudo (modo, ícone, nome do app, cor, fundo com imagem e fonte) ao padrão do app.

As escolhas ficam salvas. As fontes embutidas usam a
SIL Open Font License (texto em `FONTS-OFL.txt`).

### Estatísticas
Configurações → *Estatísticas*: total de músicas, reproduções, **tempo ouvido**, tempo da biblioteca,
favoritas e álbuns, além das **músicas, artistas e álbuns mais ouvidos** (as músicas mostram também o
tempo ouvido).
- **Tempo exato:** conta só o trecho que você realmente escutou de cada música — pausas e pulos para
  frente não contam. Vale a partir da versão 2.9.5.
- **Resetar tudo:** botão no fim da tela, com confirmação. Apaga contagens e tempo ouvido (a lista de
  recentes é mantida).

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
- Modo **claro / escuro / do sistema** e cor de destaque livre (fundo preto puro por padrão no modo escuro).
- **Tela cheia:** o app ocupa a tela toda, inclusive atrás do entalhe/câmera e da barra de status, sem
  faixa preta no topo; o conteúdo continua longe da câmera e das barras.
- Ícone adaptativo (Android 8+), com versão monocromática para o tema de ícones do Android 13+.
- Idiomas: **português (Brasil)** e **inglês**.

### Verificar atualizações
Na tela *Sobre* (Configurações → *Sobre*), o botão **Verificar atualizações** consulta o último release
do GitHub e compara com a versão instalada. Se houver versão mais nova, aparece **Baixar atualização**,
que abre o APK (`arm64-v8a`) do release no navegador; se não houver APK, abre a página do release. A
consulta só acontece quando você toca no botão, e a instalação continua sendo manual.

### Reprodução em segundo plano
Serviço de mídia (Media3): continua tocando com a tela desligada, com notificação e controles na
tela de bloqueio.

### Privacidade
Sem anúncios, sem rastreadores e sem contas. A internet é usada **só** para baixar letras que faltam
(título, artista, álbum e duração são enviados aos serviços de letras acima) e, quando você pede, para
*Buscar capa online* (o texto da busca vai para o iTunes Search) e, só quando você toca em *Verificar atualizações*, para consultar o último release no GitHub; a reprodução é 100% local. O backup é um arquivo local que só você guarda: o app não envia nada para lugar nenhum. Desligue
em Configurações → *Letras automáticas* para o app não baixar letras.

## Permissões

| Permissão | Para quê |
|---|---|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` (até Android 12) | Ler as músicas do aparelho |
| `FOREGROUND_SERVICE` (+ `_MEDIA_PLAYBACK`) e `WAKE_LOCK` | Tocar em segundo plano |
| `INTERNET` e `ACCESS_NETWORK_STATE` | Baixar letras que faltam, buscar capas online e rodar a busca de letras só quando há internet |
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
│              LyricsWorker (busca automática em segundo plano), SongCache, Labels
├── playback/  PlaybackService (sessão, ordem aleatória justa, salvamento de estado), ListenTracker (tempo
│              realmente ouvido), PlaybackQueue,
│              PlaybackState (salvar/restaurar fila), PlayerConnection, AudioFx (equalizador e aumento
│              de volume), SleepTimer (código mantido, sem acesso na interface)
└── ui/        MainActivity (abas), DetailActivity, SongPickerActivity (adicionar músicas à playlist), Backup (backup e restauração), SearchActivity, PlaylistsActivity, PlayerActivity,
               EqualizerActivity, SettingsActivity (configurações), CustomizeActivity
               (personalização avançada), BgImage (imagem de fundo),
               IconManager (troca de ícone), AppName (nome do app e atalho), StatsActivity, AboutActivity, AlphaIndexBar (barra # A–Z),
               FolderArt / FolderArtPicker (foto da pasta), SongArt / CoverSearch (capas por música),
               UiHelpers, BaseActivity (tela cheia), adaptadores e diálogos
```

## Histórico de versões

| Versão | Principais mudanças |
|---|---|
| **2.17.0** | Fila reorganizável por arrastar (com o aleatório desligado); Backup e restauração de tudo em um arquivo `.zip` salvo na pasta que você escolher |
| **2.16.0** | Foto da playlist (galeria ou capas das músicas) que substitui a mini capa na lista de playlists, com opção de remover |
| **2.15.0** | Selecionar várias músicas dentro de uma pasta e enviar para uma playlist; botão *Adicionar músicas* dentro da playlist, com busca e seleção múltipla |
| **2.14.1** | Corrige a capa do mini player parada com o Economizador de bateria ligado (rotação por quadros) |
| **2.14.0** | Capa do mini player girando devagar, estilo vinil, enquanto a música toca |
| **2.13.0** | Botão *Verificar atualizações* na tela Sobre (compara com o último release do GitHub e abre o download do APK) |
| **2.12.0** | Opção de mudar o nome do app na tela de Personalização (nome mostrado no app e atalho na tela inicial com o nome escolhido) |
| **2.11.1** | A capa personalizada também aparece na notificação de reprodução e na tela de bloqueio |
| **2.11.0** | Temas do player: Liquid glass, Preto AMOLED, Capa desfocada, Aurora, Cor sólida e Grafite (botão só no player) |
| **2.10.2** | *Cor Padrão* (azul-escuro fixo) no lugar de "usar a cor do tema"; removida a opção "usar a fonte do tema" e *Padrão do sistema* vai para o topo da lista de fontes |
| **2.10.1** | Remove o tema de vez: ficam só as personalizações (modo, ícone, cor, fundo e fonte); janela flutuante do modo com tamanho ajustado |
| **2.10.0** | *Temas* some das Configurações: tema e modo (claro/escuro) viram menus flutuantes dentro de *Personalização* (antes "Personalização avançada"); padrões novos: fundo preto puro, fonte do sistema e cor de destaque azul-escuro; mini player alinhado |
| **2.9.11** | Corrige o mini player que sumia ao voltar ao app com a música pausada |
| **2.9.10** | Liquid glass mistura as 4 cores de tons diferentes da capa |
| **2.9.9** | Tela *Sobre* atualizada com todas as funções |
| **2.9.8** | Busca de capa online só pelo nome da música |
| **2.9.7** | Busca de capas online e *Importar capa* própria no menu ⋮ do player |
| **2.9.6** | Player abre de baixo para cima |
| **2.9.5** | Estatísticas com tempo realmente ouvido e botão *Resetar tudo*; ícones de pasta sem círculo e maiores; player sem botão de temas e sem "adicionar letra" no menu ⋮ |
| **2.9.4** | *Restaurar padrões* agora pergunta antes de restaurar |
| **2.9.3** | Ao trocar de tema, cor de destaque e fonte voltam para o padrão do tema |
| **2.9.2** | Remove as cores de destaque prontas; "Cor de Destaque" só com barras (tom e intensidade) e *Usar a cor do tema*; ícone do app em menu flutuante |
| **2.9.1** | Bolinhas de cor redondas; cor livre compacta com barras de tom e intensidade; fonte em menu flutuante; remove a opção de cantos |
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
