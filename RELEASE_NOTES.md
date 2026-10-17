## MissionPlayer v2.4.2 — Chave de assinatura fixa

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.4.2-arm64-v8a.apk`.

### Mudança principal
- **Chave de assinatura padrão**: a partir desta versão todos os APKs são assinados com a **mesma chave permanente**. Antes, cada compilação gerava uma chave nova, e por isso o Android pedia para desinstalar o app antes de atualizar.
- **Importante (só desta vez)**: se você tem a v2.4.1 ou anterior instalada, **desinstale-a antes** de instalar a v2.4.2, porque elas foram assinadas com chaves diferentes. Suas playlists e favoritos dessas versões serão perdidos na desinstalação. Da v2.4.2 em diante, as atualizações são instaladas por cima normalmente.

### Alterações técnicas
- `app/build.gradle.kts`: o `signingConfig` de release lê a chave das variáveis `SIGNING_KEYSTORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` e `SIGNING_KEY_PASSWORD`; sem elas, usa a chave de debug (build local rápido).
- `.github/workflows/build-apk.yml`: o keystore vem dos Secrets do GitHub (`SIGNING_KEYSTORE_BASE64` etc.), em vez de gerar uma chave nova a cada build.
- Versão: 2.4.1 → 2.4.2 (versionCode 10).
