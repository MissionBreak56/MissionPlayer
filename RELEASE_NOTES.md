## MissionPlayer v2.5.0 — Retoma de onde parou, aleatório sem repetir e modos salvos

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.5.0-arm64-v8a.apk`.

### Novidades
- **Salva a última música**: o app lembra a fila, a música e o ponto em que estava. Ao abrir de novo, o mini player já mostra a última música, pausada no mesmo ponto; é só tocar em play. Músicas apagadas ou ocultas desde então são ignoradas.
- **Aleatório sem repetir**: no modo aleatório todas as músicas da fila tocam **uma vez** antes de qualquer uma repetir, começando pela música atual. Isso vale ao tocar uma lista, ao ligar o aleatório com música tocando e para músicas adicionadas à fila (*Tocar a seguir* entra logo depois da atual; *Adicionar à fila* entra no meio das que ainda não tocaram). Com *Repetir tudo*, cada nova rodada sorteia uma ordem nova.
- **Lembra repetir e aleatório**: a última opção escolhida de repetição (desligado / tudo / uma) e de aleatório é salva e voltam como você deixou ao reabrir o app.

### Alterações técnicas
- `PlaybackService`: ordem de aleatório própria (`DefaultShuffleOrder`) com a música atual na frente, comandos de sessão `RESHUFFLE` e `FIX_ADDED`, salvamento de repetir/aleatório e da fila/posição (a cada 5 s tocando, ao pausar, trocar de música e fechar).
- `PlaybackState` (novo), `Prefs` (modo aleatório, repetição, fila, índice e posição), `PlaybackQueue.restore`.
- `MainActivity`: restaura a fila quando o player inicia vazio.
- Versão: 2.4.2 → 2.5.0 (versionCode 11).
