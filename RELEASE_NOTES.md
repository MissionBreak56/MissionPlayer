## MissionPlayer v2.4.1 — Deslizar a capa para trocar de música

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.4.1-arm64-v8a.apk`.

### Novidade
- **Deslizar a capa no player**: na tela Tocando agora, arraste a capa para a **esquerda** para ir para a **próxima música** e para a **direita** para **voltar à anterior**. A capa acompanha o dedo e some com uma animação suave; se o arrasto for curto, ela volta ao lugar. Tocar na capa continua abrindo a letra.
- Quando não há música anterior ou seguinte na fila (com a repetição desligada), o deslize só devolve a capa ao lugar.

### Alterações técnicas
- `PlayerActivity`: novo `setupArtSwipe()` (toque na capa com limite de arrasto e animação).
- Versão: 2.4.0 → 2.4.1 (versionCode 9).
