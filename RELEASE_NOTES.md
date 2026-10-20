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
