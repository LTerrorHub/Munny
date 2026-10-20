# Munny Matrix 2.0 — Kotlin + Jetpack Compose

**Versão beta em código-fonte**, derivada do Munny Kotlin 1.0 baseado no Munny 1.4.0 corrigido. **Não é um APK já compilado**. O projeto pode ser compilado no Android Studio ou no GitHub Actions. O original (React/Capacitor) não será substituído, e as credenciais de nuvem do Munny permanecem isoladas das credenciais do Orbit.

## Experiência visual

- **Cinco abas originais:** Início, Lançamentos, Contas, Planejar, Mais.
- **Nova identidade Matrix:** fundo grafite/verde-escuro, indicadores menta/neon de alto contraste e tipografia clara; símbolos digitais caem atrás dos cartões de conteúdo, ficando **verdes no terço inicial, laranjas a partir da metade (~50%) e cinza ao aproximar-se do fim (~81%)**.
- Animação de cerca de 12 FPS, com limite de colunas. Respeita escala global de animações igual a zero nas opções de acessibilidade do Android. Opção de desligar em **Mais → Chuva digital Matrix**; preferência armazenada localmente.
- Transição em fade entre abas.
- **Privacidade (ícone de olho):** oculta e revela valores em Início, Contas, Planejar e Lançamentos; também oculta gráficos de valores quando a visão privada está ativa. Preferência persistente, sem alterar os registros financeiros. Não se trata de criptografia de dados nem proteção contra captura de tela.

## Gráficos dinâmicos na tela Início

- **Fluxo de seis meses:** barras de recebimentos confirmados vs. consumo (despesas pagas + compras no cartão). Atualiza conforme lançamentos do Munny são editados, excluídos, importados ou sincronizados.
- **Distribuição de gastos:** gráfico de rosca por categoria, com seletor de mês. Mostra valores e participações percentuais, desde que existam dados; tocar numa categoria expande os lançamentos vinculados.
- **Sem dados artificiais:** gráficos são omitidos com mensagem de estado vazio quando não há base financeira para o período.

**Regras contábeis preservadas:** saldo das contas = saldo inicial + entradas confirmadas − despesas pagas − faturas pagas; transferências entre contas têm efeito líquido nulo. Compras no cartão integram o consumo sem reduzir imediatamente o saldo bancário. Pagamentos de faturas não são somados de novo ao consumo.

## Segurança e dados

- Compatível com o formato **JSON Munny v1**, com os campos antigos preservados (incluindo campos extras).
- Usa exclusivamente o Supabase **Munny** já existente em `eputbpsvfzywqfmuwglr`, com chave **publishable** (não `service_role`), autenticação padrão e controle de revisão via `munny_commit`.
- Não modifica tabelas nem usuários do Munny ou do Orbit.
- Backup em JSON via seletor do Android; **os JSON exportados não são criptografados**. Guarde-os em local privado.
- `applicationId` = `app.munny.nativeapp`, separado do Munny original, portanto não sobrescreve sua instalação atual.

## Como compilar

### GitHub Actions

1. Crie um **repositório privado exclusivo para Munny** e extraia os arquivos **na raiz**.
2. Confira o arquivo oculto `.github/workflows/compilar-apk.yml`.
3. Envie um commit para `main` ou execute **Actions → Munny Matrix - Compilar APK Kotlin → Run workflow**.
4. O GitHub instala JDK 17, usa SDK Android 35 existente no runner e Gradle 8.13 e executa `:app:assembleDebug`.
5. Se a compilação passar, baixe `Munny-Matrix-2.0-Kotlin-debug-APK` em **Artifacts**.

### Android Studio

Abra a pasta raiz como projeto Gradle, sincronize dependências (internet necessária inicialmente), instale API 35/JDK 17 e execute `:app:assembleDebug`.

### Antes de instalar

O APK debug pode usar certificado diferente da versão anterior. **Não remova seu Munny atual sem exportar e conferir seu backup**. Faça login e valide os lançamentos com os números originais antes de migrar.

## Testes locais

```bash
kotlinc app/src/main/java/app/munny/nativeapp/FinanceMath.kt tests/FinanceMathCheck.kt -include-runtime -d finance.jar
java -jar finance.jar
kotlinc app/src/main/java/app/munny/nativeapp/FinanceMath.kt tests/MatrixAnalyticsMathCheck.kt -include-runtime -d analytics.jar
java -jar analytics.jar
```

Ambos passaram nesta entrega. Ainda faltam: compilação completa Android, inspeção visual no aparelho, testes do login e sincronização ponta a ponta, e homologação dos estados vazios e proteção visual da animação em diferentes densidades e acessibilidade.

## Limitações de paridade

O código-fonte não reproduz todas as funções do APK web original, por exemplo importação direta de backup cifrado, relatórios PDF/Excel e recorrências avançadas. Esta versão segue a implementação Kotlin 1.0 e acrescenta os recursos visuais e analíticos solicitados; não deve ser considerada uma substituta definitiva da instalação original antes dos testes no Android.
