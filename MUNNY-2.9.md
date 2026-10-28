# Munny 2.9 — Compras no cartão e gráficos selecionáveis

## Compras no cartão

Contas → Cartões passa a oferecer **Adicionar compra** e **Gerenciar lançamentos** para o cartão selecionado. A criação já abre em Compra no cartão, com o cartão preenchido e a primeira fatura calculada pelo fechamento. Gerenciar abre a lista central de Lançamentos filtrada pelo cartão, sem restrição de mês ou situação e sem repetir o histórico na área de cartões.

Em Lançamentos, cada registro mostra as ações **Editar** e **Excluir** em botões visíveis. Pagamentos de fatura mantêm a exclusão com confirmação; a edição genérica desses pagamentos não é oferecida. O botão de adicionar compra também fica disponível na lista.

A exclusão de compras parceladas permite escolher somente a parcela selecionada ou todas as parcelas do mesmo grupo de compra e do mesmo cartão. Não há agrupamento por descrição, valor ou proximidade de data. Compras importadas sem identificador de grupo só permitem exclusão individual. Faturas e limite são recalculados a partir dos registros restantes; pagamentos anteriores e compras independentes são preservados.

## Gráficos

O bloco **Gráficos resumidos** pode ser exibido, ocultado e arrastado em Personalizar Início. O Início tem uma visão compacta do mês atual e acesso direto ao detalhamento. As escolhas de assunto e tipo de gráfico são salvas separadamente para Início e Lançamentos.

- **Categorias e meios:** rosca ou barras. A legenda permite destacar uma participação. No Início, categorias menores são agrupadas em Outras categorias, preservando o total.
- **Evolução:** linhas retas ou barras agrupadas para recebido e consumo. Lacunas temporais são preenchidas com zero; o eixo começa em zero e informa seu valor máximo. Tocar no gráfico consulta os valores da data. Quando há somente um mês, o gráfico usa dias; em períodos maiores, meses.
- **Lançamentos:** aplica os mesmos filtros e busca da lista, apresenta todas as categorias, até 12 meses na evolução (com aviso quando houver recorte), tabela opcional dos valores por data e separação entre compras, despesas diretas, transferências, pagamentos e previsões.

Transferências e pagamentos de fatura ficam fora da soma de consumo. O modo de privacidade oculta os gráficos, proporções e valores. A chuva digital Matrix, os tons e a personalização existentes foram preservados.

## Verificação

Novos testes cobrem inclusão de compras e parcelas, exclusão individual e por grupo, isolamento entre cartões, preservação de pagamentos, devolução de saldo ao excluir pagamento, cartão inválido, consistência dos totais dos gráficos, agrupamento de categorias menores, lacunas e recortes temporais. As verificações automatizadas não substituem a avaliação visual e dos gestos no aparelho.
