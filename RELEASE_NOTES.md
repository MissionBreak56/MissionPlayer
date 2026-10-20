## MissionPlayer v2.13.0 — Verificar atualizações no Sobre

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.13.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidade
- **Verificar atualizações** (Configurações → Sobre): compara a versão instalada com o último release do GitHub. Havendo versão nova, o botão **Baixar atualização** abre o APK arm64-v8a do release (ou a página do release, se não houver APK). A consulta só acontece ao tocar no botão.
- Textos novos em português e inglês; a tela *Sobre*, o README e a política de privacidade foram atualizados.

### Alterações técnicas
- `AboutActivity`: consulta à API de releases do GitHub (`HttpURLConnection` + coroutines), comparação de versões numéricas e `binding` como campo da classe.
- Layout `activity_about.xml` com status e botões de atualização; novas strings `update_*` (pt/en).
- Versão: 2.12.0 → 2.13.0 (versionCode 41).

## MissionPlayer v2.12.0 — Nome do app na Personalização

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.12.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidade
- **Nome do app** (Configurações → Personalização, somente nessa tela): escolha o nome do app com *Aplicar nome* e volte ao padrão com *Nome original*. O nome aparece dentro do app, no título da tela *Sobre*.
- **Atalho com o nome escolhido:** o Android não permite que um app troque o próprio nome na gaveta de apps, então o botão *Criar atalho na tela inicial com este nome* adiciona um atalho com o nome e o ícone atuais (o launcher pede a confirmação). Depois de criado, o atalho acompanha as trocas de nome e de ícone. O nome original continua na gaveta de apps.
- *Restaurar padrões* agora também restaura o nome.
- A tela *Sobre* e o README foram atualizados.

### Alterações técnicas
- Novo `AppName` (nome escolhido e atalho fixado, via `ShortcutManagerCompat`); `Prefs.appName`; `IconManager.apply` atualiza o atalho.
- Versão: 2.11.1 → 2.12.0 (versionCode 40).
