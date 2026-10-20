# Munny Matrix 2.0 — Android nativo (Kotlin)

Repositório de desenvolvimento do aplicativo financeiro pessoal Munny Matrix, com Jetpack Compose.

## Status
- Repositório inicializado e workflow automático de importação configurado.
- **O código-fonte ainda não foi enviado:** o arquivo ZIP precisa ser carregado uma única vez pelo proprietário do repositório.
- O APK ainda **não foi compilado**, portanto não há instalação Android validada.

## Importar e compilar sem organizar arquivos manualmente

1. Baixe o arquivo **Munny-Matrix-2.0-Kotlin-Projeto.zip** entregue na conversa ChatGPT.
2. Abra este repositório na aba **Code** e escolha **Add file → Upload files**.
3. Envie o ZIP **inteiro, sem extrair e sem renomeá-lo**, diretamente para a raiz do repositório.
4. Confirme com **Commit changes** na branch `main`.
5. A execução **Importar projeto Munny Kotlin a partir do ZIP** verificará o SHA-256 do arquivo, descompactará o projeto completo, publicará os 24 arquivos nos diretórios corretos e removerá o ZIP do histórico atual.
6. O commit gerado acionará automaticamente o workflow **Munny Matrix - Compilar APK Kotlin**.
7. Após uma execução bem-sucedida, baixe o APK em **Actions → execução → Artifacts → Munny-Matrix-2.0-Kotlin-debug-APK**.

SHA-256 esperado do ZIP:
`6f6439838710b32fdd9cf85899ebde7935367475dcb9befef3b050c58aa1989f`

## Segurança
Este repositório está **público**. Não envie backups JSON com dados financeiros reais, senhas ou chaves privadas do Supabase. O aplicativo utiliza apenas a chave pública do projeto Supabase Munny. Se preferir manter o código privado, altere a visibilidade em **Settings → General → Danger Zone → Change repository visibility**.

## Funcionalidades propostas
- Cinco abas: Início, Lançamentos, Contas, Planejar e Mais
- Saldo disponível com botão para ocultar valores
- Gráficos de evolução financeira e consumo por categoria
- Chuva digital Matrix ao fundo com cores verde → laranja → cinza e opção para desativar
- Importação/exportação de backup JSON e sincronização pelo Supabase Munny

**Cuidado:** mantenha a instalação original do Munny até validar o APK nativo e conferir um backup recuperável.
