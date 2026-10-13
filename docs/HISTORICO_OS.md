# Histórico de OS — implementação Android

## Explicação para apresentação

O técnico acessa o histórico a partir de Minhas OS. A tela envia as ações ao ViewModel,
que consulta o Repository. O Retrofit faz as chamadas ao backend usando o interceptor
JWT existente. Os models representam as respostas JSON e o StateFlow informa à tela
quando mostrar carregamento, dados ou erro.

O histórico usa telas próprias, exclusivamente de consulta. Não existem comandos
de edição, exclusão, upload, reabertura ou conclusão nessas telas. O backend decide
se o técnico participou da OS como responsável ou ajudante.

## Arquivos e responsabilidades

- `data/model/HistoricoOrdemServico*.kt`: resumo, página e detalhe da resposta.
- `data/model/TipoOrdemServico.kt`: opções reais do filtro de serviço.
- `data/remote/StockFlowApi.kt`: contratos HTTP, incluindo download por streaming.
- `data/repository/OrdemServicoRepository.kt`: consultas e validação de datas/paginação.
- `ui/historico/HistoricoScreen.kt`, `HistoricoViewModel.kt`, `HistoricoUiState.kt`: listagem,
  filtros, carregamento e paginação.
- `ui/historico/HistoricoDetalhe*.kt`: detalhes, erros, download e visualização.
- `data/local/HistoricoAnexoCache.kt`: arquivos temporários e limpeza.
- `navigation/StockFlowNavigation.kt`: rotas da lista e do detalhe, retorno e login.
- `ui/ordens/OrdensScreen.kt`: entrada para o histórico.
- `ui/ordens/DetalheOrdemScreen.kt`: componentes visuais reutilizados pelo histórico.
- `res/xml/file_paths.xml`: acesso de leitura aos anexos temporários via FileProvider.

## Contrato e comportamento

- `GET /tecnico/historico`: usa somente `osId`, `dataInicialConclusao`,
  `dataFinalConclusao`, `tipoOrdemServicoId`, `page` e `size`.
- Datas são enviadas em `yyyy-MM-dd`, sempre em par. A data final inclui o dia inteiro
  conforme a regra do backend. Abertura e horários são apresentados no formato brasileiro.
- São solicitados 20 registros por página. O botão Carregar mais solicita a página seguinte.
  Aplicar filtros reinicia na página zero e cancela consultas anteriores. IDs duplicados
  são removidos ao incorporar a resposta. Falha na próxima página preserva a lista existente.
- `GET /tecnico/historico/{id}`: consulta exclusiva do histórico, inclusive para ajudantes.
- A conclusão técnica usa `fimAtendimento`; o status atual é apresentado separadamente.
  O DTO não fornece data de fechamento administrativo.
- `GET /tipos-os/ativos`: catálogo autorizado para o filtro. Tipos inativos não aparecem
  nesse seletor; OS desses tipos continuam acessíveis pela consulta sem filtro ou pelo ID.
- `GET /tecnico/historico/anexos/{anexoId}/arquivo`: JWT enviado pelo interceptor.
  Não são usadas URLs administrativas nem token em URL ou Intent.
- Imagens compatíveis são decodificadas em tamanho reduzido (até 2048 pixels por dimensão)
  para visualização com zoom. O arquivo original não é alterado no servidor.
- Outros formatos usam `ACTION_VIEW`, URI do FileProvider e permissão temporária de leitura.
  A ausência de aplicativo compatível gera uma mensagem na seção de anexos.
- O cache é privado, usa nomes locais aleatórios e remove downloads vazios, incompletos
  ou interrompidos. Imagens são removidas do disco após a leitura. Outros arquivos são
  removidos ao encerrar o detalhe. Resíduos de sessões com mais de uma hora são limpos
  no próximo download, cobrindo encerramento inesperado do processo.
- 401 solicita novo login; 403 informa acesso negado; 404 informa recurso indisponível.

## Validação automatizada

Em 13/10/2026, `assembleDebug`, `testDebugUnitTest` e `lintDebug` concluíram com sucesso.
Passaram os 14 testes novos do histórico e o teste unitário de exemplo já existente
(15 no total, zero falhas). O lint terminou com avisos não bloqueantes, incluindo a
recomendação de usar AndroidX ExifInterface no lugar da implementação da plataforma.

Executar:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Os testes `HistoricoIntegracaoTest` usam Retrofit/OkHttp com respostas interceptadas
localmente, sem acessar o servidor real. Verificam contratos, filtros, período, página
vazia, participações, paginação sem duplicados, reinício, detalhes com e sem materiais/anexos,
endpoint de arquivo, erros HTTP 401/403/404 e falha de conexão.

`HistoricoAnexoCacheTest` verifica confinamento ao cache, limpeza, arquivo vazio,
encerramento de sessão, download interrompido e tamanho incompleto.

Esses testes não validam a autorização real do backend, gestos, aparência, abertura
em aplicativos externos ou corrida de requisições na interface.

## Testes manuais pendentes

Não havia dispositivo/emulador conectado na verificação final. Validar com o backend ativo:

- Listar histórico real, vazio e com várias páginas; conferir responsável e ajudante.
- Aplicar ID, período e tipo, limpar filtros, atualizar e trocar filtros durante carregamento.
- Abrir detalhe, voltar e conferir preservação da lista/filtros.
- Conferir OS com e sem observação, materiais e anexos.
- Abrir imagem, ampliar e fechar; abrir PDF/outro formato com aplicativo compatível.
- Verificar arquivo ausente, falta de visualizador, conexão interrompida e repetição do download.
- Verificar token expirado e acesso negado com contas de teste.
- Confirmar que Minhas OS continua permitindo o fluxo de atendimento existente.

Para conectar o backend local ao dispositivo, usar `adb reverse tcp:8080 tcp:8080`.
O build debug já tenta configurar esse túnel automaticamente.
