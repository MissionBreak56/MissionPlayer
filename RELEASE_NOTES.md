## MissionPlayer v2.9.11 — Corrige o mini player que sumia ao voltar ao app

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.11-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Correção
- **Música pausada sumia ao voltar ao app**: com a música pausada, ao minimizar o app e abrir outros, o Android podia parar o serviço de música; ao voltar, o mini player desaparecia e só reaparecia fechando e abrindo o app. Agora qualquer tela do app, ao ver o player vazio, **recoloca automaticamente a última fila** (mesma música, pausada no mesmo ponto), então o mini player continua lá.

### Alterações técnicas
- `PlayerBoundActivity`: `restoreQueueIfEmpty()` (usa `PlaybackState.restoreIfEmpty`) chamado ao conectar, nos eventos do player e a cada tick, uma vez por abertura da tela e só com a biblioteca carregada.
- Versão: 2.9.10 → 2.9.11 (versionCode 34).
