# Munny 2.8 — HUD, tons e organização

O Início passa a resumir a vida financeira. Os registros e gráficos ficam em Lançamentos; Contas concentra a gestão de contas e cartões. A chuva digital Matrix foi preservada.

## O que mudou

- **Início:** nove blocos compactos, sem histórico ou gráficos detalhados. Em Configurações → Personalizar Início, escolha quais aparecem e segure o puxador para arrastar. Há rolagem nas extremidades, setas como alternativa acessível, restauração do padrão e salvamento explícito.
- **Aparência:** tons Menta, Oceano, Violeta e Âmbar nos modos Sistema, Claro e Escuro. Fundos, painéis e destaques acompanham a seleção; a chuva mantém seus caracteres e cores. A cor padrão de texto foi corrigida para evitar títulos pretos no tema escuro.
- **Lançamentos:** visões Registros e Análises, busca, ordenação e painel de filtros agrupados. Período mensal navegável, histórico completo ou intervalo personalizado com calendário; tipo, origem e situação. Gráficos e totais recebem exatamente a mesma lista filtrada, sem incluir transferências ou pagamentos de fatura no consumo. Compras no cartão seguem a regra de consumo já existente. O menu de cada registro permite editar e excluir.
- **Contas e cartões:** seleção entre as duas áreas. Cartões exibem limite cadastrado, limite consumido e disponível estimado, incluindo parcelas futuras abertas. A lista repetida de compras foi removida. A consulta de faturas e seu pagamento continuam disponíveis em seção recolhida.
- **Planejar:** abre em Metas e limites; os recursos de saúde e simulação foram mantidos. O histórico repetido foi retirado.
- **Agenda:** os eventos do dia e os próximos eventos têm ação Editar. Eventos passados e futuros podem mudar título, data, horário e notas sem gerar outro registro. A edição mantém identificador e atributos adicionais do evento.

## Dados e preferências

O formato financeiro Munny v1, os cálculos centesimais, a identidade do pacote Android e a configuração de sincronização permanecem compatíveis. Não há migração de banco de dados. Tons, ordem e visibilidade dos blocos são preferências locais persistentes; não fazem lançamentos financeiros e não são enviadas ao Cloud. Fechar o editor do HUD sem salvar descarta somente o rascunho de layout.

## Verificação

Os testes novos cobrem consistência dos filtros com totais e categorias, contas de origem e destino, pagamentos de cartão sem duplicação, intervalos inclusivos e inválidos, edição de eventos em datas passadas e futuras com preservação de identidade e campos extras, e movimentação/recuperação da ordem do HUD. A suíte financeira anterior também deve passar na compilação.

A validação interativa no aparelho deve conferir: arrastar blocos entre as extremidades e reiniciar o app; alternar os oito pares de tom/modo; aplicar e cancelar filtros; editar um evento passado e outro futuro; conferir os saldos reais antes e depois da atualização. A compilação e os testes automatizados não substituem essa inspeção no dispositivo.
