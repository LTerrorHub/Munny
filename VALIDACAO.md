# Validação — Munny Matrix 2.0

- [x] Origem: projeto Kotlin v1 criado a partir do Munny 1.4.0 corrigido.
- [x] Cinco abas mantidas; rótulos e fluxo de cadastros preservados.
- [x] Ícone de olho persistente controla ocultação de números e gráficos.
- [x] Animação Matrix na camada de fundo, opção de desligar e economia de CPU.
- [x] Gráficos alimentados exclusivamente pelas transações, sem números fictícios.
- [x] Consumo por categorias inclui apenas despesas pagas + compras no cartão (mesma competência do KPI FinanceMath.month).
- [x] Testes automatizados Kotlin do saldo/transferências/cartões/parcelas passaram.
- [x] Teste adicional: soma de categorias = consumo mensal, ignorando despesa prevista e pagamento de fatura.
- [x] Workflow usa SDK pré-instalado, removendo setup-android@v3 (falha `tools` constatada anteriormente).
- [ ] Build Android Gradle/Jetpack Compose em execução remota ou Android Studio.
- [ ] Teste real em aparelho, acessibilidade, desempenho/consumo de bateria.
- [ ] Teste de importação, login e sincronização em duas instalações.

## Critérios de aceitação no aparelho

1. Fundo com símbolos verdes até cerca da metade, laranja após a metade e cinza junto ao rodapé.
2. Desligar o fundo em Mais cessa a animação; a escolha persiste ao reabrir.
3. Botão do olho alterna os valores e gráficos sem apagar registros; a escolha persiste ao reabrir.
4. Com dados artificiais de teste, as categorias somam exatamente o consumo do mês. Transferências não contam como gasto.
5. Registrar uma compra no cartão não reduz o saldo até pagamento da fatura.
6. Acessar os gráficos por toque exibe somente os lançamentos da categoria/mês selecionados.
7. Sem registros, gráficos mostram estados vazios; não exibem dados inventados.
