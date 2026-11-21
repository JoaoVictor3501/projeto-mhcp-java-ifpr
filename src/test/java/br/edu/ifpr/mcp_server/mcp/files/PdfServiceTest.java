package br.edu.ifpr.mcp_server.mcp.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes unitários do {@link PdfService}.
 *
 * O PdfService conta as páginas de um PDF usando a biblioteca Apache PDFBox.
 *
 * Os PDFs usados nos testes são GERADOS na hora, com o próprio PDFBox, dentro
 * de uma pasta temporária (@TempDir) que o JUnit apaga ao final. Assim os
 * testes não dependem de arquivos externos nem da pasta "repositorio".
 *
 * Classes do PDFBox usadas para criar os PDFs:
 * - PDDocument: representa o documento PDF inteiro, na memória;
 * - PDPage: representa UMA página; adicionar páginas vazias ao documento
 *   basta para criar um PDF válido com a quantidade desejada;
 * - doc.save(arquivo): grava o documento em disco.
 */
public class PdfServiceTest {

   /**
    * Método auxiliar (não é um teste): cria um PDF em branco no disco.
    *
    * @param arquivo  caminho onde o PDF será salvo
    * @param paginas  quantidade de páginas em branco a criar
    */
   private void criarPdf(Path arquivo, int paginas) {
      // try-with-resources: o PDDocument é fechado automaticamente ao sair
      // do bloco, mesmo que ocorra um erro, liberando memória e o arquivo
      try (PDDocument doc = new PDDocument()) {
         for (int i = 0; i < paginas; i++)
            doc.addPage(new PDPage());
         // salva uma única vez, DEPOIS de adicionar todas as páginas
         doc.save(arquivo.toFile());
      }
      catch (IOException error) {
         // a falha aqui é na preparação do teste, e não no serviço. A
         // UncheckedIOException evita ter de declarar "throws" nos testes
         throw new UncheckedIOException("Não foi possível criar o PDF de " +
                                        "teste: " + arquivo, error);
      }
   }

   /**
    * Caminho feliz.
    *
    * Cenário:   um PDF de 3 páginas chamado "tres.pdf" na pasta temporária.
    * Ação:      countPages("tres.pdf") abre o PDF e consulta o número de
    *            páginas do documento.
    * Esperado:  o resultado é 3. Em assertEquals, o primeiro parâmetro é o
    *            valor ESPERADO e o segundo, o valor OBTIDO.
    */
   @Test
   void contarPaginasDeUmPdf(@TempDir Path pasta) {
      criarPdf(pasta.resolve("tres.pdf"), 3);

      PdfService service = new PdfService(new SafePath(pasta));

      assertEquals(3, service.countPages("tres.pdf"));
   }

   /**
    * Arquivo que não existe.
    *
    * Cenário:   a pasta temporária está vazia e a IA pede "nao-existe.pdf".
    * Ação:      o PDFBox tenta abrir o arquivo e falha com IOException; o
    *            serviço captura essa exceção e a relança embrulhada em uma
    *            UncheckedIOException (exceção "não verificada", que não
    *            obriga o chamador a declarar throws).
    * Esperado:  assertThrows confirma o tipo da exceção. O trecho "() -> ..."
    *            é uma expressão lambda: o código só roda dentro do
    *            assertThrows, que captura a exceção e confere o tipo.
    */
   @Test
   void arquivoInexistenteLancaErro(@TempDir Path raiz) {
      PdfService service = new PdfService(new SafePath(raiz));

      assertThrows(UncheckedIOException.class,
                   () -> service.countPages("nao-existe.pdf"));
   }

   /**
    * Tentativa de sair da pasta raiz.
    *
    * Cenário:   a IA pede "../fora.pdf", um arquivo na pasta PAI da raiz.
    * Ação:      antes de abrir qualquer arquivo, o serviço passa o caminho
    *            pelo SafePath, que normaliza o ".." e percebe que o resultado
    *            está fora da raiz.
    * Esperado:  PathOutsideRootException. Isto garante que a IA não consegue
    *            ler nada além da pasta autorizada.
    */
   @Test
   void caminhoForaDaRaizLancaErro(@TempDir Path raiz) {
      PdfService service = new PdfService(new SafePath(raiz));

      assertThrows(PathOutsideRootException.class,
                   () -> service.countPages("../fora.pdf"));
   }
}
