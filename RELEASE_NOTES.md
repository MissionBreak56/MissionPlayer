## MissionPlayer v2.1.0 — Polimento e nova interface

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.1.0-arm64-v8a.apk`.

### Novidades
- **Busca dentro das letras**: a pesquisa agora também encontra músicas pela letra. Lê a letra gravada no próprio arquivo (MP3/ID3 USLT, FLAC, OGG, M4A), arquivos `.lrc`/`.txt` com o mesmo nome da música e letras adicionadas no app. O trecho encontrado aparece abaixo do título.
- **Ocultar músicas e pastas**: menu ⋮ da música → *Ocultar música*; na aba Pastas, ⋮ ou toque longo → *Ocultar pasta* (inclui subpastas). Para desfazer: menu ☰ → *Itens ocultos*.
- **Letras do arquivo no player**: se a música não tiver letra anexada, o player usa a letra embutida no arquivo ou o `.lrc` vizinho.

### Interface (novo visual)
- Tela inicial redesenhada: busca em pílula, atalhos *Favoritos / Playlists / Recente*, abas em formato de pílula, fundo preto no modo escuro.
- Lista de músicas com título completo, "artista | álbum" e botão "Reprodução aleatória".
- **Álbuns em quadrados menores** (grade de 3 colunas).
- Mini player em pílula com capa circular e botão de play contornado.
- Tela "Tocando agora" redesenhada: capa grande, título à esquerda, estrela de favorito, aviso "Nenhuma letra" com botão *Adicionar*, botão de temas, controles em vidro translúcido.

### Correções
- **Bug da lista em 2 colunas**: a lista de músicas aparecia em duas colunas com títulos cortados (o layout em grade dos álbuns era reaproveitado). Corrigido.
- **Capas que não carregavam**: agora, se o Android não tiver miniatura, a capa é lida direto do arquivo de áudio; uma música sem capa não bloqueia mais as outras do mesmo álbum.
