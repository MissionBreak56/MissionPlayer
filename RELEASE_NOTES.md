## MissionPlayer v2.9.7 — Busca de capas e capa própria

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.7-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
No menu ⋮ do player, três opções novas:
- **Importar capa**: escolha uma imagem da galeria para ser a capa daquela música.
- **Buscar capa online**: pesquisa capas na internet (iTunes Search, sem conta) já com título e artista preenchidos; é só tocar numa capa para aplicar. Dá para editar o texto e pesquisar de novo.
- **Remover capa personalizada** (aparece só quando a música tem uma): volta à capa original.

A capa escolhida vale para aquela música em todo o app (listas, mini player e player) e as cores do efeito *liquid glass* acompanham a nova capa. Fica salva só no aparelho.

### Alterações técnicas
- Novos `SongArt` (armazenamento por música em `files/song_art`), `CoverSearch` (diálogo com grade de resultados) e ajustes no `ArtLoader` (a capa própria tem prioridade e muda a chave de cache).
- `PlayerActivity`: seletor de imagem, `refreshCover()` e itens no menu.
- Versão: 2.9.6 → 2.9.7 (versionCode 30).
