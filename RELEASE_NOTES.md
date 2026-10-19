## MissionPlayer v2.9.1 — Cores redondas, cor livre compacta e fonte em menu flutuante

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.1-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Bolinhas de cor redondas:** as 16 cores de destaque estavam achatadas (ovais); agora são círculos de verdade.
- **Cor livre compacta:** nova opção *Barras (compacto)* ao lado da *Roda*, com uma barra de tom (arco-íris) e uma barra de intensidade, mais uma prévia da cor. Na roda, as bolinhas de intensidade foram trocadas por uma barra.
- **Fonte em menu flutuante:** a lista longa virou um botão que abre um menu flutuante; cada fonte aparece escrita no próprio estilo e a atual vem destacada.

### Removido
- **Opção de cantos** da Personalização avançada (cada tema completo continua com o seu formato de cantos).

### Alterações técnicas
- `CustomizeActivity`: paleta com células de tamanho fixo, `wheelBlock` reescrito (roda/barras + barra de intensidade) e `ListPopupWindow` para as fontes.
- `Prefs.accentCompact` (novo); `Themes` não aplica mais o overlay de cantos.
- Versão: 2.9.0 → 2.9.1 (versionCode 24).
