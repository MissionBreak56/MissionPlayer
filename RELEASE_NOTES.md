## MissionPlayer v2.6.3 — Aleatório que nunca termina

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.6.3-arm64-v8a.apk`.

### Correções
- **Aleatório não trava mais no fim da lista**: ao chegar na última música da rodada, o botão avançar continua funcionando (e a reprodução segue sozinha). O app escolhe uma música diferente, aleatória e tocada há mais tempo, para começar uma nova rodada embaralhada, sem repetir a que acabou de tocar. Cada rodada toca todas as músicas uma vez antes de qualquer repetição.
- Com o aleatório ligado, o modo "repetir desligado" passa a se comportar como "repetir todas", já que o aleatório não deve parar.
