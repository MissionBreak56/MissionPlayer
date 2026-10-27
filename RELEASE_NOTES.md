## MissionPlayer v2.21.4 — Letras sincronizadas no editor de metadados

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.21.4-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Buscar letra online** agora procura em todas as fontes e deixa você **escolher a versão**: as letras **sincronizadas (LRC)** vêm primeiro, cada uma também com a versão em texto simples (sem tempos), e depois as de texto simples. Cada opção mostra a fonte e a primeira linha.
- A letra sincronizada é gravada **como está, com os tempos `[mm:ss]`, dentro da tag de letra** do arquivo; o app lê esse formato e mostra a letra sincronizada no player. Outros players podem mostrar os tempos como texto.
- Só uma opção encontrada e campo vazio: ela é aplicada direto. Se já houver letra no campo, a escolha mostra que ela será substituída. Nada é gravado até tocar em Salvar.

### Limitações
- A tag guarda a letra sincronizada como texto LRC (não no quadro SYLT do ID3), que é o formato que players como este leem.

## MissionPlayer v2.21.3 — Duração e pesquisa de capa no editor

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.21.3-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- O editor de metadados mostra a **duração** da música, junto com o formato, a taxa de bits e a frequência do áudio (só leitura: a duração não é uma tag).
- **Pesquisar capa online** agora abre uma caixa de pesquisa já preenchida com artista e álbum (ou música): edite o texto para procurar outra coisa. Procura no Cover Art Archive e no iTunes; toque numa capa para usá-la, e ela só é gravada ao salvar.

## MissionPlayer v2.21.2 — Editor mostra a capa original do arquivo

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.21.2-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Correção
- No editor de metadados, a capa mostrada agora é a **capa original gravada no arquivo**, e não a capa personalizada que o app guarda. Se o arquivo não tem capa incorporada, o editor avisa.
- Ao salvar uma capa nova (ou remover a capa), a capa personalizada do app é excluída e passa a valer a que você escolheu.

## MissionPlayer v2.21.1 — Letra do metadado passa a valer

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.21.1-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Correção
- Ao salvar uma **letra nova** no editor de metadados, a letra que o app tinha guardado antes (baixada ou anexada) é removida. Antes ela continuava aparecendo no lugar da letra que você acabou de gravar no arquivo. A busca por letras também passa a usar o texto novo.

## MissionPlayer v2.21.0 — Editor de metadados e busca automática

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.21.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Editar metadados** no menu de cada música (e no menu da tela do player): título, artista, artista do álbum, álbum, gênero, ano/data, faixa e total, disco e total, compositor, letrista, gravadora, álbum original, e letra (texto simples). Só aparecem os campos que o formato do arquivo realmente guarda.
- **Buscar informações automaticamente** (botão dentro da tela de edição; não fica mais nos três pontinhos) (MusicBrainz): compara título, artista, álbum, duração e ano; diferencia versões ao vivo, remixes e remasterizações; só preenche sozinho quando há um resultado claro, senão mostra a lista para você escolher, e com correspondência fraca não altera nada. O que você já digitou à mão não é sobrescrito sem perguntar.
- **Buscar letra online** na própria tela, com as mesmas fontes que o app já usa (letras sincronizadas viram texto simples; você revisa antes de salvar).
- **Buscar capa online** direto na tela (Cover Art Archive e iTunes), além de imagem do aparelho (seletor do sistema), com prévia; **remover capa** só tira a imagem das tags.
- **Gravação segura**: cópia de segurança, gravação numa cópia de trabalho, conferência da leitura e restauração do original se algo falhar. O áudio não é recodificado e as tags desconhecidas são preservadas. Formatos: MP3, FLAC, OGG/Opus e M4A/MP4; os demais ficam só para leitura.
- Depois de salvar, biblioteca, capas, notificação e a música em reprodução são atualizados.

### Limitações
- A letra gravada na tag é texto simples, sem sincronia (o app continua guardando as sincronizadas para uso offline).
- Busca em lote ainda não foi implementada.
- Android 10+ pede sua autorização por arquivo ao salvar.

### Alterações técnicas
- Nova dependência `io.github.kyant0:taglib:1.0.5` (TagLib para Android, Apache-2.0; o TagLib embutido é LGPL-2.1/MPL-1.1). Kotlin 1.9.24 → 2.1.21, necessário para ler os metadados dessa biblioteca.
- Permissão `WRITE_EXTERNAL_STORAGE` só até o Android 9 (`maxSdkVersion=28`).
- Versão: 2.20.1 → 2.21.0 (versionCode 51).

## MissionPlayer v2.20.1 — Barra de sincronia da letra sem cobrir o texto

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.20.1-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Correção
- **Ajustador de letra:** a barra com **− 0,5 s**, **+ 0,5 s**, **Auto** e **Zerar** ficava por cima da lista de letras, e as linhas passavam por trás dos botões atrapalhando a leitura. Agora a barra ocupa um espaço próprio entre a letra e o título da música: a letra termina onde a barra começa e nada passa por trás dela. Sem letra sincronizada a barra some e a letra usa o espaço todo, como antes.

### Alterações técnicas
- `activity_player.xml`: `lyricsSyncBar` saiu de dentro do `lyricsPanel` e virou irmão no `ConstraintLayout`, com o painel de letras limitado por ele.
- Versão: 2.20.0 → 2.20.1 (versionCode 50).

## MissionPlayer v2.20.0 — Sincronia automática das letras

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.20.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Ajuste automático do tempo das letras:** ao abrir uma letra sincronizada pela primeira vez, o app analisa o começo da música (volume da faixa da voz) e desloca a letra quando acha, com clareza, o ponto em que ela casa com o áudio. Na dúvida não mexe, para não estragar letras que já estavam certas. Aparece um aviso com o ajuste (ex.: *+1,2 s*). Chave em Configurações → *Ajustar sincronia das letras*.
- **Ajuste manual:** na tela da letra, botões **− 0,5 s**, **+ 0,5 s**, **Auto** (analisa na hora) e **Zerar**. O ajuste fica salvo por música.
- **Etiqueta `[offset:ms]`** do formato LRC agora é respeitada (letras baixadas e importadas); o ajuste é gravado no próprio arquivo da letra e entra no backup.
- README e tela Sobre atualizados.

### Alterações técnicas
- Novos `AudioEnvelope` (MediaExtractor/MediaCodec → energia 300–3400 Hz a cada 50 ms), `LyricAligner` (contraste de volume antes/depois do início de cada linha, com limites de confiança) e `LyricSync`.
- `LrcParser` lê `[offset]`; `LyricsStore.shiftBy/setShift`; `Prefs.lyricSyncChecked` (entra no backup).
- Versão: 2.19.0 → 2.20.0 (versionCode 49).

## MissionPlayer v2.19.0 — Mais provedores de letras

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.19.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Mais provedores de letras:** além de LRCLIB, NetEase, Kugou e lyrics.ovh, o app agora também consulta **QQ Music**, **LrcApi** e **Textyl** (letras sincronizadas) e **Lyrist** (texto simples). Mais músicas passam a ter letra automática.
- Os provedores de busca mais solta só são aceitos depois de uma conferência (título e artista batendo, ou tempos da letra compatíveis com a duração da música), para não salvar a letra errada. Um provedor fora do ar é pulado e os outros continuam.
- README e tela Sobre atualizados.

### Publicação
- A página de cada release no GitHub passa a mostrar **só as mudanças da própria versão**; o histórico completo continua no arquivo `RELEASE_NOTES.md` e na tabela do README.

### Alterações técnicas
- `LyricsFetcher`: novos provedores, conferência de duração (`fitsDuration`) e decodificação de HTML do QQ Music.
- Workflow de build: o corpo da release é extraído da seção da versão atual do `RELEASE_NOTES.md`.
- Versão: 2.18.0 → 2.19.0 (versionCode 48).

## MissionPlayer v2.18.0 — Ordem personalizada, botão na notificação e aleatório corrigido

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.18.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Ordem Personalizada:** nova opção no botão de ordenar (aba Músicas e dentro de pastas, álbuns, artistas e playlists). Cada música ganha uma alça ≡ para arrastar. A ordem é salva por lista, músicas novas entram no fim, *Tocar tudo* segue a ordem e ela entra no backup.
- **Notificação:** botão que alterna entre **aleatório** e **em sequência** (ícone de setas cruzadas = aleatório ligado; ícone de lista = em sequência).

### Correção
- **Aleatório:** músicas podiam repetir antes de todas tocarem. A rodada agora é controlada música a música: tocar uma música por qualquer caminho (fim da anterior, avançar, voltar, toque na fila) a marca como tocada; as que faltam ficam reservadas e uma nova rodada só começa quando todas tocaram, deixando as últimas ouvidas para o fim.

### Alterações técnicas
- `PlaybackService`: `syncShuffleRound` substitui `reshuffleFromCurrent`/`rotateStart` e a detecção de "volta" por índices; novo `SessionCommand` `TOGGLE_SHUFFLE` com `CommandButton` no custom layout da sessão.
- `Library.sortedCustom`/`sortedFor`, `Prefs.customOrder`, `SongReorder` (ItemTouchHelper) e alça no `item_song`.
- Versão: 2.17.0 → 2.18.0 (versionCode 47).

## MissionPlayer v2.17.0 — Reorganizar a fila e Backup/Restauração

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.17.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Reorganizar a fila:** com o aleatório desligado, arraste as músicas da fila pelo ícone ≡ (ou segure o dedo) para mudar a ordem. A música tocando continua tocando e a nova ordem é salva com a fila. Com o aleatório ligado a fila mostra o aviso para desligá-lo.
- **Backup e restauração** (Configurações → Backup): *Fazer backup* abre o seletor de arquivos para você escolher a pasta e o nome; *Restaurar backup* lê o arquivo e substitui os dados atuais. Entram favoritas, playlists e fotos, ocultos, estatísticas, aparência (inclusive imagem de fundo, ícone e nome), ordem das abas, ordenações, equalizador, aleatório/repetir, letras automáticas, fotos de pastas, capas de músicas e letras anexadas.
- As músicas são reencontradas ao restaurar pelo caminho ou por título + artista + duração, então o backup funciona também em outro aparelho; músicas que não existem mais ficam de fora e o app avisa quantas.
- README, tela Sobre (pt/en) e histórico de versões atualizados.

### Alterações técnicas
- `QueueAdapter` com `ItemTouchHelper` e `moveMediaItem` (uma chamada ao soltar o dedo).
- Nova `Backup` (zip com `backup.json` + imagens/letras, validação de nomes, restauração em pasta temporária antes de trocar os arquivos); `Prefs.allEntries()/replaceAll()`.
- Versão: 2.16.0 → 2.17.0 (versionCode 46).

## MissionPlayer v2.16.0 — Foto da playlist

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.16.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidade
- **Foto da playlist**, como a da pasta, mas aqui ela **troca a mini capa** da playlist na lista: ⋮ da playlist (tela Playlists ou dentro da playlist) → *Alterar foto da playlist* (galeria), *Escolher foto das músicas* ou *Remover foto da playlist*. A foto também aparece no cabeçalho da playlist.
- Renomear a playlist mantém a foto; apagar a playlist remove a foto. Playlists automáticas continuam sem foto personalizada.
- README, tela Sobre (pt/en) e histórico de versões atualizados.

### Alterações técnicas
- `FolderArt` reaproveitado com chave `playlist:<nome>` (não conflita com caminhos de pasta) e novo `move()` para renomeação.
- `GroupAdapter`: mini capa personalizada para playlists e ⋮ também nas playlists do usuário.
- Versão: 2.15.0 → 2.16.0 (versionCode 45).

## MissionPlayer v2.15.0 — Selecionar músicas nas pastas e adicionar músicas na playlist

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.15.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Pastas → enviar para playlist:** dentro de uma pasta, segure o dedo numa música ou use ⋮ → *Selecionar músicas*. Marque várias (ou *Todas*), toque no botão de playlist e escolha uma playlist existente ou crie uma nova. Músicas repetidas não são duplicadas e o aviso diz quantas entraram.
- **Dentro da playlist → Adicionar músicas:** ⋮ → *Adicionar músicas* abre uma lista com as músicas que ainda não estão na playlist, com busca e seleção múltipla; *Adicionar (N)* envia todas.
- README, tela Sobre (pt/en) e histórico de versões atualizados.

### Alterações técnicas
- `SongAdapter`: modo de seleção múltipla (caixinha, seleção por toque e por toque longo).
- Nova `SongPickerActivity` (+ `activity_song_picker.xml`); `SongActions.addManyToPlaylist` e `Prefs.addAllToPlaylist`.
- Versão: 2.14.1 → 2.15.0 (versionCode 44).

## MissionPlayer v2.14.1 — Correção: capa girando de verdade

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.14.1-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Correção
- **Capa do mini player girando estilo vinil:** na v2.14.0 a capa ficava parada em aparelhos com o *Economizador de bateria* ligado ou com as animações do sistema desligadas, porque o Android desativa as animações nesses casos. A rotação agora é feita quadro a quadro e funciona sempre (uma volta a cada 20 s, ~30 quadros por segundo).
- A capa mantém o ângulo ao trocar de tela e para no ângulo atual ao pausar.

### Alterações técnicas
- `PlayerBoundActivity`: `ObjectAnimator` substituído por `Choreographer.FrameCallback`, com o ângulo guardado e compartilhado entre as telas.
- Versão: 2.14.0 → 2.14.1 (versionCode 43).

## MissionPlayer v2.14.0 — Capa girando estilo vinil

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.14.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidade
- **Capa do mini player girando estilo vinil:** enquanto a música toca, a capa circular gira bem devagar (uma volta a cada 20 segundos). Ao pausar ela para no ângulo atual e retoma dali ao tocar de novo; a animação para quando o app sai de tela, sem gastar bateria.
- A tela *Sobre* e o README foram atualizados.

### Alterações técnicas
- `PlayerBoundActivity`: `ObjectAnimator` de rotação linear e infinita na `miniArt`, controlado por `isPlaying` (pause/resume) e cancelado em `onStop`.
- Versão: 2.13.0 → 2.14.0 (versionCode 42).

## MissionPlayer v2.13.0 — Verificar atualizações no Sobre

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.13.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidade
- **Verificar atualizações** (Configurações → Sobre): compara a versão instalada com o último release do GitHub. Havendo versão nova, o botão **Baixar atualização** abre o APK arm64-v8a do release (ou a página do release, se não houver APK). A consulta só acontece ao tocar no botão.
- Textos novos em português e inglês; a tela *Sobre*, o README e a política de privacidade foram atualizados.

### Alterações técnicas
- `AboutActivity`: consulta à API de releases do GitHub (`HttpURLConnection` + coroutines), comparação de versões numéricas e `binding` como campo da classe.
- Layout `activity_about.xml` com status e botões de atualização; novas strings `update_*` (pt/en).
- Versão: 2.12.0 → 2.13.0 (versionCode 41).

## MissionPlayer v2.12.0 — Nome do app na Personalização

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.12.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidade
- **Nome do app** (Configurações → Personalização, somente nessa tela): escolha o nome do app com *Aplicar nome* e volte ao padrão com *Nome original*. O nome aparece dentro do app, no título da tela *Sobre*.
- **Atalho com o nome escolhido:** o Android não permite que um app troque o próprio nome na gaveta de apps, então o botão *Criar atalho na tela inicial com este nome* adiciona um atalho com o nome e o ícone atuais (o launcher pede a confirmação). Depois de criado, o atalho acompanha as trocas de nome e de ícone. O nome original continua na gaveta de apps.
- *Restaurar padrões* agora também restaura o nome.
- A tela *Sobre* e o README foram atualizados.

### Alterações técnicas
- Novo `AppName` (nome escolhido e atalho fixado, via `ShortcutManagerCompat`); `Prefs.appName`; `IconManager.apply` atualiza o atalho.
- Versão: 2.11.1 → 2.12.0 (versionCode 40).
