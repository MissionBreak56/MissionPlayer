## MissionPlayer v2.7.0 — Letras automáticas, estatísticas e 7 novos temas

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.7.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Letras automáticas:** o app procura sozinho as letras das músicas que não têm, em segundo plano e só quando há internet, e salva no aparelho para funcionar **offline**. Consulta **LRCLIB**, **NetEase**, **Kugou** e **lyrics.ovh**; a primeira letra sincronizada vence e o texto simples é a alternativa. Ao abrir uma música sem letra, ela é buscada na hora. As letras baixadas também entram na busca por letra. Menu ⋮ → **Letras automáticas** liga ou desliga (vem ligado).
- **Estatísticas** (menu ⋮ → *Estatísticas*): total de músicas, reproduções, tempo ouvido, favoritas, álbuns e os mais ouvidos (músicas, artistas e álbuns).
- **7 novos temas completos**, cada um com cores, fonte e cantos próprios: Neon, Papel, Oceano, Pôr do sol, Mono, Floresta e Rosé.

### Mudou
- O app agora tem permissão de internet (`INTERNET` e `ACCESS_NETWORK_STATE`), usada somente para baixar letras. Título, artista, álbum e duração da música são enviados aos serviços de letras; desligue a função no menu para o app não usar a rede.

### Alterações técnicas
- `LyricsFetcher` (novo): fontes de letra com cooldown por fonte e escolha pela duração; `LyricsWorker` (novo): WorkManager a cada 15 min, 4 músicas em paralelo, só com internet.
- `StatsActivity` (nova) e `UiHelpers.kt` (novo, auxiliares de telas feitas em código).
- Temas T8–T14 em `themes.xml`, `colors.xml`, `arrays.xml` e `Themes.kt`.
- Dependência nova: `androidx.work:work-runtime-ktx` 2.9.1.
- Versão: 2.6.7 → 2.7.0 (versionCode 21).
