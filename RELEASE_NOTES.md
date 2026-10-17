## MissionPlayer v2.6.2 — Correção do toque que não respondia

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.6.2-arm64-v8a.apk`.

### Correções
- **Toque ignorado ao voltar de outra tela** (ex.: sair do player e tocar em uma pasta): ao retornar, o app recarregava a biblioteca e reconstruía todas as listas mesmo sem nenhuma mudança, o que "engolia" o primeiro toque. Agora a biblioteca só atualiza as listas quando algo realmente mudou, e as listas não são redesenhadas se o conteúdo é o mesmo.
