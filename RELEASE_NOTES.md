## MissionPlayer v2.9.5 — Estatísticas exatas, pastas sem círculo e player mais limpo

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.5-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Estatísticas com tempo exato**: o tempo ouvido agora conta só o trecho que você realmente escutou de cada música (pausas e pulos para frente não contam). Aparece em horas e minutos (ou minutos e segundos), no total e ao lado de cada música do ranking.
- **Botão "Resetar tudo"** no fim das Estatísticas, com janela de confirmação. Apaga contagens de reproduções e tempo ouvido (a lista de recentes é mantida).

### Mudanças
- **Ícones de pasta** (e gênero/playlist vazia): sem o círculo/fundo atrás e um pouco maiores.
- **Player**: removido o botão de temas.
- **Menu ⋮ do player**: removida a opção de adicionar arquivo de letra (continua no botão de letra); *Remover letra* continua.

### Alterações técnicas
- Novo `ListenTracker` (soma a posição reproduzida a cada segundo, ignora saltos) e `Prefs.listenedMs/addListened/resetStats`; `PlaybackService` chama o tracker.
- `StatsActivity`: tempo ouvido real, `MaterialAlertDialogBuilder` no reset.
- `Adapters`: fundo transparente e padding 6dp nos ícones; `activity_player.xml` sem `btnThemes`.
- Versão: 2.9.4 → 2.9.5 (versionCode 28).
