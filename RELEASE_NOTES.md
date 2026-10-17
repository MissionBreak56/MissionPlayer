## MissionPlayer v2.4.0 — Foto da pasta pelas músicas e aumento de volume

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.4.0-arm64-v8a.apk`.

### Novidades
- **Foto da pasta a partir das músicas**: no menu da pasta (aba Pastas ou botão ⋮ dentro da pasta) há a nova opção **Escolher foto das músicas**. Ela mostra as capas das músicas daquela pasta; é só tocar numa para usá-la como foto da pasta. Capas repetidas do mesmo álbum aparecem uma vez e músicas sem capa não são listadas. Escolher da galeria continua funcionando.
- **Aumentar volume**: na tela do **Equalizador** há o controle **Aumentar volume**, que eleva o som além do máximo normal (até cerca de +15 dB). Funciona sem precisar ligar o equalizador, e o botão **Redefinir** também o zera. Valores altos podem distorcer o som e prejudicar a audição ou o alto-falante.

### Alterações técnicas
- Novo `FolderArtPicker.kt` e layout `item_pick_art.xml`; `FolderArt.saveBitmap` e `ArtLoader.decodeSync`.
- `DetailActivity`: botão ⋮ também em pastas (alterar foto, escolher das músicas, remover foto).
- `AudioFx`: `LoudnessEnhancer` ligado à sessão de áudio (`Prefs.fxGain`, limite de 1500 mB).
- `activity_equalizer.xml` e `EqualizerActivity`: slider de volume; textos novos em inglês e português.
- Versão: 2.3.2 → 2.4.0 (versionCode 8).
