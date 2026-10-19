## MissionPlayer v2.10.1 — Sem temas, só personalização

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.10.1-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Mudanças
- **Temas removidos de vez**: sai a escolha de tema (os de cor e os completos como Neon e Papel). Na tela **Personalização** ficam apenas **Modo** (Do sistema / Claro / Escuro), **Ícone do app**, **Cor de Destaque**, **Cor do fundo** (e imagem da galeria) e **Fonte**.
- Quem estava usando outro tema volta ao **visual padrão** (preto puro, fonte do sistema, destaque azul-escuro); as demais escolhas continuam salvas.
- **Janela flutuante do modo**: agora tem só o tamanho necessário para as 3 opções, em vez de ocupar quase a tela toda.
- Tela **Sobre**, README e texto de *Restaurar padrões* atualizados.

### Alterações técnicas
- `CustomizeActivity`: remove `themeSection()`; `showListMenu` usa `WRAP_CONTENT` para listas curtas.
- `Prefs.migrateThemeRemoval()` (chamado em `App`): zera o tema salvo e recoloca o destaque e a fonte padrão.
- Versão: 2.10.0 → 2.10.1 (versionCode 36).
