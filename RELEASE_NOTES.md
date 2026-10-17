## MissionPlayer v2.6.7 — Listas aparecem mais rápido

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.6.7-arm64-v8a.apk`.

### Correções
- **Listas demoravam para aparecer** (e mostravam "Nada por aqui ainda" enquanto carregavam). Agora:
  - o app guarda uma **cópia da biblioteca** e mostra as listas na hora ao abrir, enquanto atualiza em segundo plano com o que mudou no aparelho;
  - as listas de músicas, álbuns, artistas e pastas são montadas **fora da tela principal**, então o app não trava ao carregar;
  - enquanto as listas são montadas aparece **"Carregando…"** em vez da mensagem de lista vazia;
  - ao reabrir a tela inicial com a biblioteca já carregada, as listas são mostradas direto, sem esperar uma mudança.
- **Ordenação bem mais rápida** (A a Z, artista, álbum): o texto sem acentos de cada item passa a ser calculado uma só vez, em vez de a cada comparação.
- **Abrir uma pasta, álbum ou artista** ficou mais rápido: as coleções são montadas uma vez por mudança na biblioteca e reaproveitadas.
- O app não lê mais os gêneros do aparelho a cada abertura (uma consulta por gênero, sem uso desde que a aba Gêneros saiu).

### Alterações técnicas
- `SongCache` (novo): cópia binária da lista de músicas em `filesDir`, lida antes da consulta ao MediaStore e regravada quando algo muda.
- `Library`: `Derived` (álbuns, artistas, pastas e A a Z calculados sob demanda e guardados), `sortKey` com caminho rápido para texto ASCII e regex única, `letterOf` só normaliza a primeira letra, `load()` sem `loadGenres`.
- `LibraryPagerAdapter`: `refresh()` monta um `Snapshot` em `Dispatchers.Default` e só então atualiza os adaptadores; novo estado `ready` e `release()`.
- `MainActivity`: `pages.refresh()` na criação quando a biblioteca já está carregada; `pages.release()` no `onDestroy`.
- Versão: 2.6.6 → 2.6.7 (versionCode 20).
