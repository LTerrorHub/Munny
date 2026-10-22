# Munny 2.5 — Saúde Financeira e Simulador de Investimentos

## Planejar: três áreas acessíveis por chips
- **Saúde:** entradas confirmadas, consumo registrado, margem mensal, comprometimento, evolução de seis meses e compromissos.
- **Simulador:** valor inicial, aporte recorrente, taxa mensal ou anual efetiva, prazo entre 1 e 600 meses, projeção recalculada ao editar, comparativo capital aportado vs juros.
- **Metas e limites:** mantém orçamentos por categoria, objetivos e lançamentos previstos existentes.

## Definições dos indicadores
- **Comprometimento da renda:** (cartão lançado + despesas diretas pagas no mês) / receitas efetivamente recebidas no mês. Sem entradas confirmadas, o percentual é indisponível, nunca 0% fictício.
- **Margem do mês:** entradas confirmadas menos consumo registrado. **Não** equivale ao saldo bancário nem a valor garantido para investir.
- **Saldo em contas:** saldos iniciais mais movimentos financeiros efetivamente pagos até a data; transferências entre contas se cancelam.
- **Posição líquida estimada:** saldo em contas menos faturas de cartão registradas e ainda abertas até a data de referência. **Não é patrimônio líquido total**; investimentos, bens, empréstimos e dívidas não cadastradas não estão representados.
- **Evolução:** reconstrução dos seis últimos meses usando os saldos iniciais atuais das contas e os lançamentos informados. Não afirmar que representa um extrato histórico de patrimônio real.
- **Alertas:** faturas vencidas ou com vencimento nos próximos 30 dias, despesas planejadas e limites de orçamento atingidos/ultrapassados. Alertas na tela; ainda não são notificações push.
- **Aporte ilustrativo:** 50% do menor excedente mensal entre os três meses completos anteriores, desde que pelo menos dois tenham entradas confirmadas. É apenas um valor de preenchimento do simulador, não uma indicação financeira.

## Modelo matemático do investimento
Com aporte ao fim de cada mês:

`saldo[n] = saldo[n-1] × (1 + i_m) + aporte_mensal`

Para taxa informada como anual **efetiva**: `i_m = (1 + i_a)^(1/12) - 1`.

O app distingue investimento acumulado (principal + aportes) de juros simulados. O resultado não considera tributos, corretagem, inflação, risco, variação da rentabilidade ou garantias de retorno; valores são cenários matemáticos, não cotações e não são persistidos.

## Privacidade e compatibilidade
- Não adiciona endpoints, tabelas ou credenciais. Permanece no Supabase Munny atual.
- O simulador não registra transações nem investimentos reais.
- Ao ocultar valores, os gráficos, números, porcentagens de saúde e campos monetários do simulador são ocultados.
- Motor financeiro existente e abas Início/Lançamentos/Contas/Agenda mantidos.
- `applicationId = app.munny.nativeapp`, `versionName=2.5.0`, `versionCode=250`.

## Testes
CI roda JUnit (juros compostos com/sem aportes, taxa anual efetiva, validação de limites, saldo e cartões sem dupla contagem, renda inexistente) e `assembleDebug`/`assembleRelease`. Confirme a assinatura antes da instalação e faça backup JSON.
