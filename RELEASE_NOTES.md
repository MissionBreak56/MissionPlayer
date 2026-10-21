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
