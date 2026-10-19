## MissionPlayer v2.10.2 — Cor Padrão e fontes

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.10.2-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Mudanças
- **Cor de Destaque**: o botão "Usar a cor do tema" virou **Cor Padrão** e aplica a **cor fixa do app** (azul-escuro, a mesma do visual padrão). Quando ela está ativa, o botão fica marcado e o texto mostra "Cor padrão do app". Quem ainda estava com "cor do tema" passa para a Cor Padrão.
- **Fonte**: removida a opção **"Usar a fonte do tema"**; **"Padrão do sistema"** agora é a **primeira** da lista (e continua sendo a fonte padrão). Quem usava "fonte do tema" passa para a fonte do sistema.
- README atualizado.

### Alterações técnicas
- `Prefs.accent` e `Prefs.fontStyle` passam a mapear os valores antigos (-1 e 0) para `DEFAULT_ACCENT` e `DEFAULT_FONT`.
- `CustomizeActivity`: botão Cor Padrão; lista de fontes com ordem própria (índices salvos não mudam).
- Versão: 2.10.1 → 2.10.2 (versionCode 37).
