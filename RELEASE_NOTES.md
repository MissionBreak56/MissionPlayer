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
