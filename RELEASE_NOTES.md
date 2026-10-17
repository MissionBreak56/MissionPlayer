## MissionPlayer v2.3.2 — Ícone corrigido

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.3.2-arm64-v8a.apk`.

### Correção principal
- **Ícone do app**: na v2.3.1 o ícone novo não carregava e o Android mostrava o robozinho genérico (no instalador e na tela inicial). O desenho das notas tinha um erro de formato e foi refeito. Agora o ícone aparece corretamente.

### Ícone novo
- Nota musical dupla em degradê de laranja-avermelhado para vermelho vivo, sobre fundo escuro com leve brilho avermelhado.
- Ícone adaptativo (Android 8+), com versão monocromática para o tema de ícones do Android 13+, e PNGs para Android 6–7.
- O mesmo ícone aparece na tela **Sobre**.

### Correções visuais (vindas da v2.3.1)
- **Botão "Aleatório"**: o texto não quebra mais em duas linhas. *Reproduzir* e *Aleatório* dividem a largura e o texto se ajusta ao espaço.
- **Barra de progresso do player**: a bolinha não fica mais cortada no início da música (0:00).
- **Título longo no player**: as bordas do texto em rolagem têm um degradê suave, em vez de corte seco.

### Alterações técnicas
- `drawable-v24/ic_launcher_foreground.xml` e `ic_launcher_monochrome.xml`: `pathData` das cabeças das notas corrigido (arcos com 7 parâmetros).
- Ícone: `mipmap-anydpi-v26/v33`, PNGs em `mipmap-*dpi` e `drawable-nodpi/ic_launcher_art.png`; `AndroidManifest.xml` aponta `icon` e `roundIcon` para `@mipmap`.
- `activity_detail.xml` e `activity_player.xml`: ajustes de layout descritos acima.
- Versão: 2.3.1 → 2.3.2 (versionCode 7).
