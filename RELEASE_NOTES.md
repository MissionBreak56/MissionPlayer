## MissionPlayer v2.8.0 — Barra alfabética nas pastas, ordenação e personalização avançada

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.8.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Barra alfabética (# A–Z) dentro de álbuns, pastas, artistas, gêneros e playlists**, com a letra em destaque no centro. Aparece em listas com mais de 12 músicas quando a ordem é A a Z, Artista ou Álbum.
- **Ordenar dentro das listas:** novo botão de ordenar no topo de cada álbum/pasta/artista/playlist, com *Ordem padrão* e as mesmas opções da lista de Músicas (A a Z, Z a A, Mais recentes, Mais tocadas, Artista, Álbum, Duração). A escolha fica salva por tipo de lista e *Tocar tudo* / *Aleatório* seguem a ordem escolhida.
- **Personalização avançada** (menu ⋮ → *Personalização avançada*):
  - **Ícone do app:** 9 opções (Original, Verde, Azul, Violeta, Dourado, Rosa, Vinil, Ondas e Fones), com versão monocromática para o Android 13+.
  - **Cor de destaque:** 16 cores no lugar da cor do tema.
  - **Cor do fundo:** Padrão, Puro, Grafite, Azul-noite, Sépia e Musgo, cada um claro e escuro.
  - **Fonte:** 9 opções, aplicadas em todo o app.
  - **Cantos:** Padrão, Retos, Suaves ou Redondos.
  - **Restaurar padrões** (volta inclusive o ícone original).

### Observação sobre o ícone
Ao atualizar para esta versão, alguns launchers podem tirar o atalho do app da tela inicial (a entrada de lançamento agora é um alias). O app continua na gaveta de apps; basta arrastar o ícone de volta.

### Alterações técnicas
- `CustomizeActivity` e `IconManager` (novos); alias de lançador por ícone no manifesto (`activity-alias`).
- Estilos `Overlay.*` (cor, fonte, cantos, fundo) aplicados sobre o tema em `BaseActivity`; versões `values-night` para o modo escuro.
- `DetailActivity`: `AlphaIndexBar` e ordenação por tipo de lista (`Prefs.detailSort`).
- Versão: 2.7.0 → 2.8.0 (versionCode 22).
