## MissionPlayer v2.10.0 — Personalização unificada e novos padrões

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.10.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Mudanças
- **"Temas" removido das Configurações.** O **Tema** (os 8 de cor e os 7 completos) e o **Modo** (Do sistema / Claro / Escuro) agora são **menus flutuantes** no topo da tela de personalização, no mesmo estilo do menu de fontes.
- **"Personalização avançada" agora se chama "Personalização"**.
- **Novos padrões** (para quem ainda não tinha escolhido nada): **fundo preto puro**, **fonte do sistema** e **cor de destaque azul-escuro** (tom 220°, intensidade *Escuro*).
- A opção de fundo **"Padrão" foi removida**; quem usava "Padrão" passa para **Puro**. *Restaurar padrões* também volta tema e modo.
- **Mini player alinhado**: margens esquerda e direita iguais, espaços iguais entre play, próxima e fila, e centralização vertical corrigida.

### Alterações técnicas
- `ThemesActivity`, `ThemeAdapter` e layouts removidos; `CustomizeActivity` ganha `themeSection()` e `modeSection()` com `ListPopupWindow`.
- `Prefs`: `DEFAULT_ACCENT = 544`, `DEFAULT_FONT = 9`, `DEFAULT_BG = 1` (valor salvo 0 vira 1).
- `layout_mini_player.xml`: paddings e tamanhos dos botões simétricos.
- Versão: 2.9.11 → 2.10.0 (versionCode 35).
