## MissionPlayer v2.6.5 — Tela cheia, player que não fecha e ícone de lista

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.6.5-arm64-v8a.apk`.

### Mudanças
- **Tela cheia**: o app agora ocupa a tela toda, inclusive atrás do entalhe/câmera e da barra de status, sem a faixa preta no topo. O fundo preenche tudo e o conteúdo continua longe da câmera e das barras. No player, a cor da capa também preenche o topo.
- **Ícone da lista de reprodução/fila**: agora só com linhas, sem a nota musical (fila no player e no mini player, e lista de reprodução).

### Correções
- **Player fechando ao voltar de outra aba**: quando o sistema parava o serviço de música (por exemplo com a música pausada) e você voltava ao app, a fila ficava vazia e a tela do player fechava na hora. Agora o player recoloca a última fila (música e posição, pausada) e só fecha se continuar vazia depois de alguns segundos.

### Alterações técnicas
- `PlaybackState.restoreIfEmpty()`: restaura a última fila salva quando o player está vazio; usado pela `PlayerActivity`, que não chama mais `finish()` na hora com a fila vazia (espera 4 s).
- `BaseActivity`: `setDecorFitsSystemWindows(false)`, `layoutInDisplayCutoutMode=shortEdges`, tratamento de insets (barras, entalhe e teclado); barras de status e navegação transparentes no tema.
- `ic_queue.xml` e `ic_playlist.xml` redesenhados só com linhas.
- Versão: 2.6.4 → 2.6.5 (versionCode 18).
