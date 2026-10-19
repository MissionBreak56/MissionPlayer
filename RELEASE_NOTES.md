## MissionPlayer v2.9.2 — Cor de Destaque simplificada e ícone em menu flutuante

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.2-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Mudanças
- **Cor de Destaque:** a seção com as 16 bolinhas de cores prontas foi removida. A antiga "Cor livre" agora se chama **Cor de Destaque** e mantém a opção *Usar a cor do tema*.
- **Só barras:** a roda de cores foi apagada; ficam a barra de tom (arco-íris), a barra de intensidade e a prévia da cor.
- **Ícone do app em menu flutuante:** igual ao das fontes, um botão mostra o ícone atual e abre uma lista com a imagem e o nome de cada ícone.
- Quem usava uma das 16 cores prontas mantém a mesma cor, agora como cor personalizada.

### Alterações técnicas
- `ColorWheelView` removido; `Themes.migrateLegacyAccent()` converte a cor pronta salva (chamado no `App`); `Prefs.accentCompact` removido.
- `CustomizeActivity`: `accentSection` reescrita (tema ou barras) e `showIconMenu` com `ListPopupWindow`.
- Versão: 2.9.1 → 2.9.2 (versionCode 25).
