## MissionPlayer v2.9.0 — Configurações, roda de cores, imagem de fundo e mais fontes

APK já compilado (arm64-v8a) em anexo: `MissionPlayer-v2.9.0-arm64-v8a.apk`. Atualiza por cima da versão anterior (mesma chave de assinatura).

### Novidades
- **Tela de Configurações:** o menu flutuante da tela inicial foi substituído por uma engrenagem que abre uma tela própria, com tudo organizado em seções: *Aparência* (Temas e Personalização avançada), *Biblioteca* (Organizar abas e Itens ocultos), *Áudio e letras* (Equalizador e a chave de Letras automáticas) e *Mais* (Estatísticas e Sobre).
- **Roda de cores** na cor de destaque: arraste no círculo para escolher qualquer tom e toque numa das 5 intensidades (pastel, suave, vivo, forte e escuro). Cada escolha tem versão clara e escura. As 16 cores prontas e *Usar a cor do tema* continuam disponíveis.
- **Imagem da galeria como fundo:** em *Cor do fundo* agora dá para escolher uma foto da galeria, ajustar a visibilidade (ela se mistura com a cor do fundo para o texto continuar legível), trocar ou remover. A tela *Tocando agora* mantém o visual liquid glass da capa.
- **Mais fontes:** de 9 para 19 opções. Novas do sistema: *Padrão do sistema, Fina, Versalete, Condensada leve, Condensada média* e *Serifa mono*. Novas embutidas (SIL Open Font License): *Arredondada (Fredoka), Manuscrita (Amatic), Rabisco (Intuitive), Serifa elegante (Source Serif)* e *Moderna (Hanken Grotesk)*.
- **Usar a fonte do tema:** nova opção na fonte, igual à da cor, que mantém a fonte própria de cada tema (a opção *Padrão* virou *Padrão do sistema*, uma fonte explícita).

### Correção
- **A fonte não mudava com alguns temas** (os temas completos T8 a T14 definem a própria fonte no estilo dos textos e ela vencia a escolha do usuário). Agora a fonte escolhida vale em qualquer tema.

### Outras mudanças
- README atualizado (a seção de compilação foi removida).
- Restaurar padrões também remove a imagem de fundo.

### Alterações técnicas
- `SettingsActivity`, `ColorWheelView` e `BgImage` (novos); `Themes` com catálogo de fontes (`FontOption`) e 360 overlays gerados para a roda de cores (`accent_wheel.xml`, claro e escuro).
- `Overlay.Font*` agora também substituem `android:textViewStyle`; fontes embutidas em `res/font` e estilos em `values-v26`.
- `Prefs`: `bgImageStamp` e `bgImageVisible`; versão 2.8.0 → 2.9.0 (versionCode 23).
