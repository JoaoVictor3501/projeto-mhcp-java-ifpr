package br.edu.ifpr.mcp_server.mcp.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;

/**
 * Procura arquivos dentro da pasta raiz usando um padrão "glob", a mesma
 * notação de curingas usada em linha de comando:
 * - *   qualquer sequência de caracteres, SEM atravessar pastas
 *       (ex.: "*.pdf" encontra os PDFs que estão direto na raiz);
 * - **  qualquer sequência de caracteres, atravessando pastas;
 * - {a,b}  qualquer uma das opções listadas (ex.: "{*.pdf,*.txt}").
 *
 * Atenção: um padrão com dois asteriscos seguidos de barra exige que exista
 * pelo menos uma pasta antes do arquivo, então NÃO encontra arquivos soltos na
 * raiz. Para pegar a raiz e as subpastas, use "{*.pdf," seguido do mesmo
 * padrão com dois asteriscos e barra, fechando com "}".
 *
 * A busca nunca sai da raiz: os caminhos são sempre percorridos a partir dela.
 */
public class GlobService {
   // define a pasta raiz da busca (e garante que ela é absoluta e normalizada)
   private final SafePath safePath;

   /**
    * @param safePath raiz segura onde as buscas serão realizadas
    */
   public GlobService(SafePath safePath) {
      this.safePath = safePath;
   }

   /**
    * Busca arquivos cujo caminho (relativo à raiz) combine com o padrão.
    *
    * @param padrao        padrão glob (ex.: "*.pdf")
    * @param maxResultados quantidade máxima de arquivos devolvidos
    * @return caminhos RELATIVOS à raiz (ex.: "docs/manual.pdf")
    * @throws UncheckedIOException se a pasta não puder ser percorrida
    * @throws java.util.regex.PatternSyntaxException se o padrão for inválido
    */
   public List<Path> search(String padrao, int maxResultados) {
      // pasta raiz, já absoluta e normalizada pelo SafePath
      Path raiz = safePath.getRoot();
      // PathMatcher é o objeto do Java que responde "este caminho combina com
      // o padrão?". O prefixo "glob:" diz qual sintaxe de padrão será usada
      PathMatcher matcher = FileSystems.getDefault()
                                       .getPathMatcher("glob:" + padrao);
      // Files.walk percorre a raiz e todas as subpastas, devolvendo um Stream
      // (fluxo) de caminhos. O try-with-resources fecha o Stream ao final,
      // liberando as pastas que o Java mantém abertas no disco
      try (Stream<Path> arquivos = Files.walk(raiz)) {
         return arquivos
            // mantém somente arquivos de verdade (descarta pastas)
            .filter(Files::isRegularFile)
            // converte para caminho relativo à raiz, por exemplo,
            // ".../repositorio/docs/a.pdf" vira "docs/a.pdf". É sobre este
            // caminho que o padrão é aplicado e que o resultado é mostrado
            .map(raiz::relativize)
            // mantém só os que combinam com o padrão informado
            .filter(matcher::matches)
            // respeita o limite de resultados pedido
            .limit(maxResultados)
            // transforma o fluxo em uma lista (imutável)
            .toList();
      }
      catch (IOException e) {
         // embrulha a exceção verificada (IOException) em uma não verificada,
         // para que quem chama não precise declarar "throws"
         throw new UncheckedIOException(e);
      }
   }
}
