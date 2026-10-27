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
  por **A a Z, Z a A, Mais recentes, Mais tocadas, Artista, Álbum, Duração ou Personalizada**. A ordem
  alfabética ignora acentos (Á fica junto do A).
- **Ordem Personalizada:** escolha *Personalizada* no botão de ordenar e cada música ganha uma alça (≡)
  à direita: arraste pela alça para colocá-la onde quiser. A ordem é salva **por lista** (a aba
  Músicas e cada pasta, álbum, artista ou playlist têm a sua), *Tocar tudo* e tocar uma música seguem
  essa ordem, músicas novas entram no fim da lista e a ordem entra no backup. Na primeira vez a aba
  Músicas começa em A a Z e as outras listas começam na ordem própria delas.
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
Tocar a seguir, adicionar à fila, adicionar à playlist, favoritar, definir como toque, compartilhar,
ocultar, **Editar metadados**, **Substituir capa** e **Remover capa** (veja *Editor de metadados*).

### Editor de metadados
Menu ⋮ da música (ou do player) → **Editar metadados**. Grava as tags **dentro do arquivo** de áudio
(MP3, FLAC, OGG/Opus e M4A/MP4; os outros formatos ficam só para leitura).
- **Duração:** o editor mostra a duração da música, o formato, a taxa de bits e a frequência (só leitura).
- **Campos:** título, artista, artista do álbum, álbum, gênero, ano/data, faixa e total, disco e total,
  compositor, letrista, gravadora, álbum original e letra (texto simples). Só aparecem os campos que o
  formato do arquivo realmente guarda.
- **Buscar informações automaticamente** (botão dentro da tela): consulta o **MusicBrainz** comparando
  título, artista, álbum, duração e ano, e diferencia versões ao vivo, remixes e remasterizações. Com um
  resultado claro, preenche os campos; com vários possíveis, mostra a lista para você escolher; com
  correspondência fraca, não altera nada. Campos que você já digitou não são sobrescritos sem perguntar.
- **Buscar letra online:** usa as mesmas fontes das letras automáticas; letras sincronizadas viram texto
  simples e você revisa antes de salvar.
- **Capa:** mostra a capa original gravada no arquivo (não a personalizada do app); ao salvar uma capa nova ou remover a capa, a personalizada do app é excluída. *Escolher imagem* (seletor do sistema), *Pesquisar capa online* (caixa de pesquisa editável; Cover Art Archive e iTunes) e
  *Remover capa*. A imagem é validada e reduzida para no máximo 1200 px; só a capa frontal é trocada.
- **Nada é gravado até você tocar em Salvar.** A gravação é segura: cópia de segurança, gravação numa
  cópia de trabalho, conferência da leitura e restauração do original se algo falhar. O áudio não é
  recodificado e as tags que o editor não conhece são preservadas. No Android 10+ o sistema pede a sua
  autorização para cada arquivo modificado.
- Ao salvar uma **letra nova**, a letra que o app guardava antes (baixada ou anexada) é removida para a do arquivo valer.
- Depois de salvar, a biblioteca, as capas, a notificação e a música em reprodução são atualizadas.

Ainda não existe edição em lote, e a letra gravada na tag não é sincronizada (o app continua guardando
as letras sincronizadas para uso offline).

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
- **Sem repetições antes da volta completa:** o app registra cada música que começa, seja pelo fim da
  anterior, por *avançar*, *voltar* ou por um toque na fila. As que ainda não tocaram nessa rodada
  ficam reservadas, então nenhuma música repete antes de todas as outras terem tocado. Voltar percorre
  as que acabaram de tocar e avançar retoma de onde parou. Ao começar uma nova rodada as últimas
  músicas ouvidas vão para o fim dela, para não voltarem logo.
- **Modos salvos:** a última escolha de repetir e de aleatório volta como você deixou.

### Botão aleatório/sequência na notificação
A notificação de reprodução (e os controles da tela de bloqueio) ganhou um botão que **alterna entre
aleatório e em sequência**: o ícone de setas cruzadas indica que o aleatório está ligado e o ícone de
lista indica que as músicas tocam em ordem. Ele aparece junto dos controles de voltar, tocar/pausar e
avançar (em algumas versões do Android, ao expandir a notificação) e acompanha o botão do player.

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
- **Áudio e letras:** *Equalizador* e as chaves de *Letras automáticas* e *Ajustar sincronia das letras*.
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
  consultando em ordem **8 provedores**: **LRCLIB**, **NetEase**, **Kugou**, **QQ Music**, **LrcApi**,
  **Textyl** (sincronizadas), **lyrics.ovh** e **Lyrist** (texto simples). A primeira letra
  sincronizada vence; texto simples só entra se nenhuma fonte tiver a versão sincronizada.
- Os provedores que buscam de forma mais solta (LrcApi, Textyl e Lyrist) passam por uma conferência:
  só são aceitos se o título e o artista batem, ou se os tempos da letra cabem na duração da música,
  para não salvar a letra de outra música. Um provedor fora do ar ou que limita pedidos é pulado por um
  tempo e os outros continuam. A cada busca só vão título, artista, álbum e duração.
- As letras baixadas ficam salvas no aparelho e funcionam **offline**, inclusive na busca por letra.
  Ao abrir uma música sem letra, ela é buscada na hora.
- A busca roda a cada 15 minutos (o menor intervalo que o Android permite), com 4 músicas ao mesmo
  tempo; músicas não encontradas são tentadas de novo após 15 minutos.
- Nada para configurar. Para desligar: Configurações → *Letras automáticas*.

### Sincronia das letras
Letras de outras versões da música (ao vivo, edição de rádio, intro mais longa) podem ficar adiantadas
ou atrasadas. O app corrige isso de três formas:
- **Automática:** na primeira vez que uma letra sincronizada aparece, o app decodifica os primeiros 2:40
  da música, mede o volume da faixa da voz e procura o deslocamento em que o começo de cada linha cai
  onde o som "sobe" (a voz entrando depois da introdução, de um solo ou de uma pausa). Só aplica quando
  o resultado é **claramente melhor** que a letra como está e que qualquer outro deslocamento; na
  dúvida não mexe, então uma letra que já estava certa não é alterada por palpite. Aparece o aviso
  *Letra ajustada automaticamente: +1,2 s*. Cada música é analisada uma vez; letra nova (baixada de
  novo ou importada) é analisada de novo. Para desligar: Configurações → *Ajustar sincronia das letras*.
- **Manual:** com a letra aberta, uma barra logo abaixo da letra (sem ficar por cima dela) tem **− 0,5 s** (a letra adianta),
  **+ 0,5 s** (a letra atrasa), **Auto** (roda a análise na hora) e **Zerar** (volta ao que o arquivo
  diz). O ajuste fica salvo para aquela música.
- **Etiqueta `[offset:ms]`:** o padrão LRC agora é respeitado, tanto nas letras baixadas quanto nas
  importadas. O ajuste é gravado no próprio arquivo da letra (por isso entra no backup).

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
*Buscar capa online* (o texto da busca vai para o iTunes Search), no *Editor de metadados* (título, artista, álbum e duração vão para o MusicBrainz e o Cover Art Archive, e o texto da capa para o iTunes) e, só quando você toca em *Verificar atualizações*, para consultar o último release no GitHub; a reprodução é 100% local. O backup é um arquivo local que só você guarda: o app não envia nada para lugar nenhum. Desligue
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
| `WRITE_EXTERNAL_STORAGE` (só até Android 9) | Gravar as tags no *Editor de metadados*; no Android 10+ o sistema pede autorização por arquivo |

## Tecnologia

Kotlin 2.1 · AndroidX · Material Components 1.12 · Media3 (ExoPlayer, MediaSession) 1.4.1 · Palette ·
View Binding · TagLib para Android (`io.github.kyant0:taglib`, leitura e gravação de tags) · `minSdk 23` · `targetSdk 34` · `compileSdk 34`.

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
├── tags/      TagModel (campos e regras das tags), TagStore (leitura e gravação segura com TagLib), CoverImage
├── online/    MusicBrainz, MatchScorer (pontuação das correspondências), CoverArt, Itunes, Http
└── ui/        MainActivity (abas), DetailActivity, SongPickerActivity (adicionar músicas à playlist), Backup (backup e restauração), SearchActivity, PlaylistsActivity, PlayerActivity,
               EqualizerActivity, SettingsActivity (configurações), CustomizeActivity
               (personalização avançada), BgImage (imagem de fundo),
               IconManager (troca de ícone), AppName (nome do app e atalho), StatsActivity, AboutActivity, AlphaIndexBar (barra # A–Z),
               FolderArt / FolderArtPicker (foto da pasta), SongArt / CoverSearch (capas por música),
               MetadataEditorActivity / MetadataEditorViewModel / SongRefresh (editor de metadados),
               UiHelpers, BaseActivity (tela cheia), adaptadores e diálogos
```

## Histórico de versões

| Versão | Principais mudanças |
|---|---|
| **2.21.3** | O editor de metadados mostra a duração (e formato, taxa de bits e frequência) e a busca de capa online passa a ter uma caixa de pesquisa editável |
| **2.21.2** | O editor de metadados mostra a capa original gravada no arquivo (não a personalizada do app); ao salvar uma capa nova, a personalizada do app é excluída |
| **2.21.1** | Salvar uma letra nova no editor de metadados remove a letra que o app guardava antes, para a letra gravada no arquivo passar a valer |
| **2.21.0** | Editor de metadados: grava tags e capa no arquivo (MP3, FLAC, OGG/Opus, M4A), busca automática no MusicBrainz, busca de letra e de capa online, gravação com cópia de segurança e conferência; Kotlin 2.1 |
| **2.20.1** | Corrige a barra de ajuste de sincronia da letra, que ficava por cima das linhas: agora ela ocupa um espaço próprio abaixo da letra |
| **2.20.0** | Sincronia automática das letras (análise do áudio quando há certeza), botões − 0,5 s / + 0,5 s / Auto / Zerar e suporte à etiqueta `[offset]` |
| **2.19.0** | Mais provedores de letras (QQ Music, LrcApi, Textyl e Lyrist, além de LRCLIB, NetEase, Kugou e lyrics.ovh); a página de cada release no GitHub mostra só as mudanças da própria versão |
| **2.18.0** | Ordem *Personalizada* (arraste para organizar cada lista); botão aleatório/sequência na notificação; corrige músicas que se repetiam no aleatório antes de dar a volta completa |
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
