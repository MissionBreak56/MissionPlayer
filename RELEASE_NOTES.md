## MissionPlayer v2.11.1 — Capa personalizada na notificação

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.11.1-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Mudança
- **Notificação de reprodução** (e tela de bloqueio) agora mostram a **capa personalizada** da música (importada ou buscada online). Ao trocar ou remover a capa no player, a notificação é atualizada na hora; sem capa própria, continua a capa do álbum.

### Alterações técnicas
- Novo `SongBitmapLoader` (carregador de capas da sessão de mídia): as músicas passam a usar o endereço `missionart://song/<id>?album=<albumId>`, que resolve a capa própria ou a do MediaStore, sem cache.
- Novo comando de sessão `REFRESH_ART` (chama `onUpdateNotification`), enviado pelo `PlayerActivity` depois de mudar a capa.
- Versão: 2.11.0 → 2.11.1 (versionCode 39).
