# Roteador principal na conclusão técnica

## Fluxo implementado

1. O detalhe técnico lê modelo, propriedade e data de atualização em `GET /tecnico/os/{id}`.
2. A seção Roteador principal mostra o cadastro atual. Se estiver ausente ou incompleto,
   exige modelo e propriedade. Se completo, permite manter ou atualizar opcionalmente.
3. A digitação consulta `GET /clientes/roteador-principal/sugestoes?q=...&limite=10`
   após 350 ms de pausa. Consultas antigas são canceladas; entradas vazias não geram HTTP.
   Escolher uma sugestão apenas preenche o formulário. Digitação livre continua disponível
   se o autocomplete falhar. A ordenação recebida do servidor é preservada.
4. A confirmação mostra os dados que serão enviados. O ViewModel valida observação,
   modelo (até 150 caracteres, após trim) e propriedade, e faz um único PATCH no endpoint
   técnico. Manter o cadastro omite `roteadorPrincipal` do JSON.
5. O sucesso do PATCH atualiza imediatamente o status da OS. Um GET separado recarrega
   os dados normalizados e a data definidos pelo servidor. Falha nesse GET mostra
   “Atendimento concluído” e oferece repetir apenas a consulta, sem reenviar a conclusão.

A transação, o histórico de alterações e o aprendizado dos modelos pertencem ao backend.
O Android não chama os endpoints administrativos de edição/histórico do cliente.
Não há alteração das regras de estoque, materiais, ajudantes ou conferência.
As gravações de materiais, anexos e ajudantes não podem ocorrer simultaneamente com a conclusão.
O usuário que alterou o roteador não é mostrado porque o DTO técnico não fornece esse campo.

## Arquivos

- `data/model/RoteadorPrincipal.kt`: propriedade, request e sugestão.
- `data/model/OrdemServicoDetalhe.kt`: dados atuais opcionais.
- `data/model/ConclusaoAtendimentoRequest.kt`: objeto opcional na conclusão.
- `data/remote/StockFlowApi.kt`, `data/repository/OrdemServicoRepository.kt`: acesso à API.
- `ui/ordens/RoteadorConclusaoUiState.kt`: rascunho e validação do formulário.
- `ui/ordens/RoteadorConclusaoSection.kt`: apresentação do cadastro e campos.
- `ui/ordens/DetalheOrdemScreen.kt`, `DetalheOrdemUiState.kt`, `DetalheOrdemViewModel.kt`:
  integração com conclusão, sugestões, erros e atualização do detalhe.

## Validação em 15/10/2026

`assembleDebug`, `testDebugUnitTest` e `lintDebug` concluíram com sucesso.
Passaram 27 testes unitários no total, incluindo 12 testes novos do roteador.
Os testes verificam JSON omitido/aninhado, endpoints, parâmetros, limites, sugestões,
leitura dos campos, cadastro parcial, manutenção e atualização opcional.
São testes locais com respostas simuladas; não comprovam execução contra o servidor real.

Não havia emulador/celular conectado. Permanecem pendentes os testes manuais:
cliente sem cadastro, manter cadastro, trocar só propriedade/modelo, escolher sugestão,
modelo novo, digitação rápida, perda de conexão, erros 400/401/403, rotação, recarga após
sucesso e regressão do fluxo de materiais/anexos/ajudantes.
