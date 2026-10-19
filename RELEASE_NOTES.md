## MissionPlayer v2.9.10 — Liquid glass com mais cores da capa

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.10-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Mudança
- **Mesclagem de cores do liquid glass**: antes o efeito pegava só 3 cores parecidas (uma capa com muito verde, azul e roxo podia ficar toda vermelha por causa de um título). Agora o app separa as **4 cores de tons diferentes** que mais aparecem na capa e as mistura no fundo do **player em tela cheia** (gradiente + manchas de luz em cantos diferentes) e no **mini player**, com transição animada entre músicas.

### Alterações técnicas
- Novo `GlassColors.pick()`: ordena as cores da capa por presença e escolhe até 4 com tons distantes (≥ 30°), ignorando cinzas, preto e branco quando há opção; completa com as mais comuns se a capa tiver poucos tons.
- `PlayerActivity` e `PlayerBoundActivity` passam a usar 4 cores (gradiente e animações).
- Versão: 2.9.9 → 2.9.10 (versionCode 33).
