## MissionPlayer v2.3.1 — Novo ícone e correções visuais

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.3.1-arm64-v8a.apk`.

### Novo ícone
- Novo ícone do app: nota musical dupla em degradê de laranja-avermelhado para vermelho vivo, sobre fundo escuro com leve brilho avermelhado.
- Ícone adaptativo (Android 8+), com versão monocromática para o tema de ícones do Android 13+, e PNGs para Android 6–7.
- O mesmo ícone aparece na tela **Sobre**.

### Correções
- **Botão "Aleatório"** (tela de playlist/álbum/pasta): o texto não quebra mais em duas linhas ("Aleat / ório"). Os botões *Reproduzir* e *Aleatório* dividem a largura e o texto se ajusta ao espaço disponível.
- **Barra de progresso do player**: a bolinha não fica mais cortada pela metade no início da música (0:00); a barra continua alinhada com o texto.
- **Título longo no player**: as bordas do texto em rolagem agora têm um degradê suave, em vez de um corte seco.

### Alterações técnicas
- `activity_detail.xml`: botões com largura por peso, `maxLines=1` e `autoSizeTextType`.
- `activity_player.xml`: `SeekBar` com padding de 12dp e margens negativas equivalentes; `infoPanel` com `clipChildren/clipToPadding=false`; título com `fadingEdge`.
- Ícone: `mipmap-anydpi-v26/v33`, `drawable-v24` (fundo, primeiro plano e monocromático), PNGs em `mipmap-*dpi` e `drawable-nodpi/ic_launcher_art.png`; removido `drawable/ic_launcher.xml`.
- `AndroidManifest.xml`: `icon` e `roundIcon` apontam para `@mipmap`.
- Versão: 2.3.0 → 2.3.1 (versionCode 6).
