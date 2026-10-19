## MissionPlayer v2.9.6 — Player abre de baixo

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.6-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Mudança
- **Animação do player**: ao tocar no mini player, a tela Tocando agora agora sobe **de baixo** para cima. Ao fechar, ela desce de volta.

### Alterações técnicas
- Novas animações `slide_up_in`, `slide_down_out` e `stay`; `PlayerBoundActivity.openPlayer()` e `PlayerActivity.finish()` usam `overridePendingTransition`.
- Versão: 2.9.5 → 2.9.6 (versionCode 29).
