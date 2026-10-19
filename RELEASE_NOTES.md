## MissionPlayer v2.9.3 — Trocar de tema volta cor e fonte para o padrão do tema

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.3-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Mudança
- **Ao trocar de tema**, as opções *Cor de Destaque* e *Fonte* voltam para *Usar a cor do tema* e *Usar a fonte do tema*, e o novo tema aparece com o visual padrão dele. Depois você pode personalizar de novo em Personalização avançada. Tocar no tema que já está ativo não muda nada.

### Alterações técnicas
- `ThemesActivity`: ao escolher um tema diferente, `Prefs.accent = -1` e `Prefs.fontStyle = 0`.
- Versão: 2.9.2 → 2.9.3 (versionCode 26).
