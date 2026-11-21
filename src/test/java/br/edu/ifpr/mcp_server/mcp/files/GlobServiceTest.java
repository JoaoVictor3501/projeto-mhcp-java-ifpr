package br.edu.ifpr.mcp_server.mcp.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes unitários do {@link GlobService}.
 *
 * O GlobService procura arquivos dentro da pasta raiz usando um padrão "glob",
 * a mesma notação de curingas usada em linha de comando:
 * - *   qualquer sequência de caracteres, SEM atravessar pastas
 *       (ex.: "*.pdf" = PDFs da pasta raiz);
 * - **  qualquer sequência de caracteres, atravessando pastas
 *       (ex.: "**" + "/*.pdf" = PDFs dentro de subpastas);
 * - {a,b}  qualquer uma das opções listadas.
 *
 * Recursos do JUnit 5 e de java.nio.file usados nos testes:
 * - @TempDir: o JUnit cria uma pasta temporária, entrega ao método como
 *   parâmetro e a apaga automaticamente ao final do teste. Assim os testes não
 *   deixam lixo e não dependem da pasta "repositorio" real;
 * - Files.createFile / Files.createDirectory: criam um arquivo vazio e uma
 *   pasta no disco (dentro da pasta temporária);
 * - assertEquals(esperado, obtido): falha se os dois valores forem diferentes.
 */
public class GlobServiceTest {

   /**
    * Busca de PDFs dentro de uma subpasta.
    *
    * Cenário:   a pasta temporária tem um .txt na raiz e uma pasta "desenho"
    *            contendo um .txt e um .pdf.
    * Ação:      search("**" + "/*.pdf", 100) procura PDFs dentro de pastas, em
    *            qualquer nível. O segundo parâmetro (100) é o limite máximo
    *            de resultados.
    * Esperado:  somente o PDF é devolvido, como caminho RELATIVO à raiz
    *            ("desenho/vinlandsaga.pdf"); os .txt são ignorados.
    */
   @Test
   void acharPdfs(@TempDir Path pasta) throws IOException {
      Files.createFile(pasta.resolve("bob_o_bobo.txt"));
      Files.createDirectory(pasta.resolve("desenho"));
      Files.createFile(pasta.resolve("desenho/pequeno_principe.txt"));
      Files.createFile(pasta.resolve("desenho/vinlandsaga.pdf"));

      // o serviço só enxerga a pasta temporária: é o SafePath que define a raiz
      GlobService service   = new GlobService(new SafePath(pasta));
      List<Path>  resultado = service.search("**/*.pdf", 100);

      assertEquals(List.of(Path.of("desenho/vinlandsaga.pdf")), resultado);
   }

   /**
    * A "pegadinha" do glob.
    *
    * Cenário:   um PDF solto diretamente na raiz ("a.pdf"), sem nenhuma pasta.
    * Ação:      busca com dois padrões diferentes.
    *            - "*.pdf"  ACHA o arquivo, pois o * cobre o nome dele;
    *            - "**" + "/*.pdf" NÃO acha, porque a barra depois dos dois
    *              asteriscos exige que exista UMA PASTA antes do nome.
    * Esperado:  o primeiro padrão devolve [a.pdf], e o segundo devolve uma
    *            lista vazia. Para pegar os dois casos de uma vez, usa-se
    *            o padrão "{*.pdf,**" + "/*.pdf}".
    */
   @Test
   void pdfNaRaizSoApareceComPadraoSemPasta(@TempDir Path raiz)
                                                         throws IOException {
      Files.createFile(raiz.resolve("a.pdf"));
      GlobService service = new GlobService(new SafePath(raiz));

      // o padrão sem pasta ACHA o PDF solto na raiz...
      assertEquals(List.of(Path.of("a.pdf")), service.search("*.pdf", 10));
      // ...e o padrão com "**/" NÃO acha
      assertEquals(List.of(), service.search("**/*.pdf", 10));
   }
}