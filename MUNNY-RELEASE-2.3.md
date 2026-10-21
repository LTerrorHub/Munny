# Munny 2.3 — Release e identidade visual

## O que mudou
- O seletor de mês sobre o gráfico de rosca tem botões de 52 dp, com contorno, fundo, sombra suave e maior espaçamento antes do gráfico.
- Ícone adaptativo redesenhado: monograma "M" em verde esmeralda com moeda dourada, com camada monocromática para ícones temáticos do Android.
- O cabeçalho do Munny usa o mesmo emblema.
- Nenhum cálculo financeiro, dado, sincronização Supabase, chuva Matrix, modo claro/escuro, aba Agenda ou navegação foi modificado.
- Versão Android `2.3.0` (`versionCode=230`, `applicationId=app.munny.nativeapp`).

## Distribuição
O CI executa `assembleDebug` para validar o código e `assembleRelease` para criar uma versão não depurável. Quando uma chave release está configurada nos secrets, o Gradle assina o APK; caso contrário, publica `release-unsigned`, que **não pode ser instalado** sem assinatura posterior.

## Assinatura permanente e futuras atualizações
Configure nas configurações de **Settings > Secrets and variables > Actions** os quatro secrets:

| Nome do secret | Conteúdo |
| --- | --- |
| `MUNNY_KEYSTORE_BASE64` | base64 do arquivo PKCS12 privado de assinatura |
| `MUNNY_STORE_PASSWORD` | senha do arquivo keystore |
| `MUNNY_KEY_ALIAS` | alias da chave |
| `MUNNY_KEY_PASSWORD` | senha privada da chave |

A chave e as senhas **nunca** devem ser colocadas no repositório GitHub. Guarde uma cópia offline em local seguro. Sem esse material, não será possível assinar futuras atualizações compatíveis.

Uma versão release assinada com uma chave diferente da usada por instalações debug anteriores não substituirá essas instalações diretamente. **Antes de desinstalar**, exporte o backup JSON e confirme a sincronização com Supabase Munny.

## Validação
- Compilar a PR e a branch `main` no GitHub Actions.
- Verificar assinatura v2 do APK, pacote e versão.
- Testar no dispositivo os dois temas, botões do seletor mensal, menu de categorias e o ícone no launcher.
- Release assinado não significa aplicativo publicado na Google Play. Essa publicação requer conta de desenvolvedor, compliance e processo de distribuição separado.
