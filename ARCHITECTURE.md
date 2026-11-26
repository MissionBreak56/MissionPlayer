# Arquitetura do MissionPlayer

Um único módulo Gradle (`:app`), Kotlin, telas em Views/XML (e algumas montadas em código), `minSdk 23`.
Não há banco de dados: o estado do usuário fica em `SharedPreferences` e em arquivos da pasta privada do app, e
a biblioteca de músicas vem do **MediaStore** (o app nunca copia nem move as músicas).

```
app/src/main/java/com/missionbreak56/musicplayer/
├── data/      biblioteca, preferências, letras
├── playback/  serviço de reprodução, fila, estatísticas de escuta
├── ui/        telas, adaptadores, diálogos, capas
├── tags/      leitura e gravação de tags dentro dos arquivos (TagLib)
├── online/    MusicBrainz, Cover Art Archive, iTunes
└── update/    atualização dentro do app
```

## Papel de cada pacote

### `data` — estado e fontes de dados
| Peça | Função |
|---|---|
| `MusicRepository` | Lê o MediaStore e devolve `List<Song>`. Marca `lastLoadFailed` se a permissão sumiu ou o MediaStore falhou, para uma leitura ruim não ser confundida com "biblioteca vazia". |
| `Library` | Singleton que guarda a lista atual (`songs`, `albums`, `artists`, `folders`), avisa os ouvintes quando muda e decide quando recarregar. Mostra o cache do disco primeiro e atualiza em seguida. |
| `SongCache` | Cópia da biblioteca em arquivo para o app abrir já com a lista. |
| `Prefs` | Todas as preferências do usuário (favoritos, playlists, estatísticas, ocultos, aparência…). Favoritos, playlists, recentes e contagens têm **cache em memória**; toda escrita passa por aqui para o cache não ficar velho. |
| `Lyrics` (`LyricsStore`, `LyricsIndex`, `EmbeddedLyrics`) | Letras salvas pelo app (`files/lyrics/<id>.lrc`), letras lidas de dentro dos arquivos e o índice de busca por letra. |
| `LyricsFetcher` / `LyricsWorker` | Fontes online de letras (com pausa quando uma fonte pede para esperar) e a tarefa em segundo plano (WorkManager) que busca as que faltam. |
| `LyricSync`, `LyricAligner`, `AudioEnvelope` | Ajuste de tempo das letras sincronizadas. |

### `playback` — o que toca
- **`PlaybackService`** (`MediaSessionService` + ExoPlayer): único dono do player. Mantém a sessão de mídia, a
  notificação, o equalizador (`AudioFx`), o timer de sono e o modo aleatório sem repetição.
- **`PlaybackQueue`**: comandos personalizados da sessão (aleatório/sequência, atualizar capa, atualizar música
  editada…) e as regras da fila.
- **`PlaybackState`**: salva e restaura a fila, a música e a posição (a posição é gravada a cada 5 s, só tocando).
- **`PlayerConnection`**: o `MediaController` que as telas usam para falar com o serviço.
- **`ListenTracker`**: conta reproduções e tempo realmente ouvido (alimenta as estatísticas).
- **`SongBitmapLoader`**: capa que aparece na notificação.

### `ui` — telas
Cada tela é uma `Activity` que estende `BaseActivity` (tema, cor de destaque, fonte, bordas do sistema).
`PlayerBoundActivity` acrescenta a conexão com o player. Pontos de partida: `MainActivity` (abas, mini player,
verificação de atualização ao abrir), `PlayerActivity` (tela Tocando agora, letras, fila), `DetailActivity`
(álbum, artista, pasta, playlist), `SearchActivity`, `SettingsActivity`/`CustomizeActivity`/`AboutActivity` e
`MetadataEditorActivity` (+ `MetadataEditorViewModel`). Capas: `ArtLoader` (cache de bitmaps), `SongArt`
(capa personalizada), `FolderArt`. `Backup` cria e restaura o arquivo de backup. `UpdateFlow` são as janelas da
atualização.

### `tags` — gravar tags nos arquivos
`TagStore` lê e grava com **TagLib** sem recodificar o áudio: copia o arquivo, edita a cópia, confere a leitura,
só então substitui o original (e restaura a cópia de segurança se algo falhar). `TagCodec` (puro, testável)
converte entre campos do editor e propriedades do TagLib; `CoverImage` valida e reduz imagens de capa.

### `online` — fontes na internet
`MusicBrainzClient` (busca e detalhes, com pausa de 1 s entre pedidos), `MatchScorer` (pontua e classifica as
correspondências), `CoverArtClient` (Cover Art Archive), `ItunesCoverClient` e `Http` (conexão https com limite
de tamanho, redirecionamentos e cancelamento). Nada é gravado a partir daqui: só devolve dados.

### `update` — atualização dentro do app
`AppUpdater` consulta o último release no GitHub, baixa o APK para o cache e confere tamanho, checksum e
pacote/versão; entrega ao `PackageInstaller`. `UpdateInstallReceiver` recebe o resultado.

## Fluxos de dados

```
MediaStore ──► MusicRepository ──► Library ──(ouvintes)──► telas (adaptadores)
                                      ▲  └──► SongCache (disco)
                       SongRefresh ───┘   (depois de salvar tags: reescaneia, recarrega, invalida capas)

telas ──► PlayerConnection ──(MediaController)──► PlaybackService ──► ExoPlayer
                                                      │
                                       Prefs / PlaybackState / ListenTracker

editor ─► TagStore (TagLib, arquivo) ─► SongRefresh ─► Library + PlaybackService (recarrega a música tocando)
editor ─► online/* (MusicBrainz, CAA, iTunes, fontes de letra) ─► campos do editor (nada gravado até "Salvar")
MainActivity ─► AppUpdater ─► UpdateFlow ─► PackageInstaller
```

## Decisões e limites atuais

- **Sem injeção de dependência.** `Library`, `Prefs` e afins são `object`s usados pelo app inteiro. Adotar
  Hilt exigiria transformar cada um em componente injetável, mexer em todas as telas e no serviço e incluir
  KSP no build: é uma refatoração grande e sem rede de segurança de testes de tela. Os pontos que precisam
  de teste já são puros (`TagCodec`, `MatchScorer`, parsers de JSON, `AppUpdater.parse`) ou recebem as
  dependências por parâmetro (`MusicBrainzClient(HttpFetcher)`). Se o projeto crescer, o caminho é extrair
  interfaces para `Prefs` e `Library` primeiro e só então avaliar Hilt/Koin.
- **Um módulo.** Dividir em `:core`, `:data`, `:feature…` só compensa depois de desfazer os acoplamentos entre
  `ui`, `data` e `playback` (hoje as telas usam os singletons diretamente). Os pacotes `tags`, `online` e
  `update` já são independentes das telas e seriam os primeiros candidatos a módulos.
- **Sem Paging 3.** A biblioteca inteira cabe em memória (e em `SongCache`) e o RecyclerView lida bem com
  milhares de linhas; Paging precisaria de uma fonte paginada (o MediaStore é lido de uma vez, ordenado e
  filtrado em memória). Vale rever com bibliotecas de dezenas de milhares de músicas.
- **R8 ligado** na versão release (código reduzido e ofuscado, recursos não usados removidos); regras em
  `app/proguard-rules.pro`. O TagLib precisa ficar sem renomeação (o código nativo o chama por nome).
- **Perfil de inicialização (Baseline Profile)** escrito à mão em `app/src/main/baseline-prof.txt` e instalado
  por `androidx.profileinstaller`; não foi gravado com Macrobenchmark.

## Qualidade

- **Testes de JVM** (`app/src/test`): regras de tags, MusicBrainz (leitura, pontuação, cliente com rede falsa),
  Cover Art Archive, iTunes, letras e atualizador. Rodam com `gradle :app:testDebugUnitTest`.
- **Testes instrumentados** (`app/src/androidTest`): gravação real de tags (MP3, FLAC, OGG, M4A), capa,
  tela principal, leitura do MediaStore, favoritos e backup. Precisam de um aparelho/emulador **arm64**
  (`gradle :app:connectedDebugAndroidTest`); o CI só confirma que eles compilam.
- **Análise estática:** ktlint e detekt rodam no GitHub Actions (`.github/workflows/lint.yml`), com as regras em
  `.editorconfig` e `config/detekt/detekt.yml`; os achados antigos do detekt ficam em
  `config/detekt/baseline.xml`, então só código novo é cobrado.
