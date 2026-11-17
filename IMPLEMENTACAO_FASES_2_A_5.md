# Guitar Finder — Fases 2 a 5

Esta branch foi criada a partir de `mais-modelos-sons` e NÃO altera a `main` do fork nem o repositório do professor.

## Mecânica implementada
- Exploração: deslize horizontalmente na câmera AR até completar a barra de progresso; o sistema escolhe uma das guitarras GLB existentes.
- A guitarra aparece na cena. Toque em **Investigar guitarra** para responder um quiz com três títulos.
- Angus Young: Thunderstruck (coautoria com Malcolm Young); Steve Vai: For the Love of God; Jim Root: Psychosocial (coautoria).
- Acerto: +10 pontos e efeito curto de confirmação via ToneGenerator. Erro: -1 vida.
- Três erros: Game Over com motivo, pontuação e botão para jogar novamente.
- Pontos de encontro geográfico: cada modelo GLB novo encontrado por GPS vale +10 uma vez por partida; a seleção manual de modelos não pontua.
- Localização: painel com endereço, raio de 100 m e saída de 125 m, seguindo o controlador feature-location.
- Cadastrados 5 pontos: Catedral Sant'Ana, Igreja dos Polacos, Vila Velha, Praça Barão do Rio Branco, e região do Lago de Olarias.
- Observação: a coordenada do Lago de Olarias deve ser conferida no Maps antes de uma entrega final. A exploração por gesto mede deslocamento na tela; a câmera AR real acompanha o movimento físico do dispositivo.

## Testes necessários no Android Studio / aparelho
1. Sincronizar Gradle e compilar; correções podem ser necessárias conforme versão do SceneView.
2. Conceder câmera e localização precisa, validar visualização AR em celular compatível.
3. Deslizar sobre a cena até revelar uma guitarra, posicionar o GLB e investigar.
4. Testar as três alternativas e confirmar som, vida e +10 no acerto.
5. Errar três quizzes, conferir Game Over e reinício.
6. Validar endereço, cinco pontos e ausência de pontuação repetida na mesma partida.
7. Capturar screenshots e vídeo no celular; não são gerados automaticamente no GitHub.

## Limitação de acesso
O arquivo RAR local enviado na conversa não pôde ser inspecionado nesta execução; a implementação usa a branch `mais-modelos-sons` do GitHub como base. Os modelos presentes nela foram preservados.
