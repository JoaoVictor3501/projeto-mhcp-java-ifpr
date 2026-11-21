package br.edu.ifpr.mcp_server.mcp.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Conta as páginas de arquivos PDF usando a biblioteca Apache PDFBox.
 *
 * Todo caminho recebido passa antes pelo {@link SafePath}, então o serviço só
 * consegue abrir arquivos que estejam dentro da pasta raiz autorizada.
 */
public class PdfService {
   // raiz segura: converte o texto recebido em um caminho dentro da raiz.
   // 'private' porque ninguém fora desta classe precisa acessá-la
   private final SafePath safePath;

   /**
    * @param safePath raiz segura onde os PDFs serão procurados
    */
   public PdfService(SafePath safePath) {
      this.safePath = safePath;
   }

   /**
    * Conta as páginas de um PDF.
    *
    * @param caminho caminho do PDF, relativo à raiz (ex.: "manual.pdf")
    * @return quantidade de páginas do documento
    * @throws PathOutsideRootException se o caminho sair da raiz
    * @throws UncheckedIOException se o arquivo não existir ou não puder ser
    *         lido como PDF
    */
   public int countPages(String caminho) {
      // transforma o texto em um caminho absoluto, garantidamente dentro da
      // raiz; se tentar sair dela, lança PathOutsideRootException AQUI, antes
      // de qualquer tentativa de abrir o arquivo
      Path arquivo = safePath.resolve(caminho);
      // Loader.loadPDF abre o PDF e devolve um PDDocument (o documento inteiro,
      // na memória). O try-with-resources fecha o documento automaticamente ao
      // final, mesmo que ocorra um erro
      try (PDDocument doc = Loader.loadPDF(arquivo.toFile())) {
         // pergunta ao documento quantas páginas ele tem
         return doc.getNumberOfPages();
      }
      catch (IOException error) {
         // arquivo inexistente, corrompido ou que não é um PDF: embrulha a
         // exceção verificada em uma não verificada, com mensagem clara
         throw new UncheckedIOException("Não foi possível ler o PDF: " +
                                        caminho, error);
      }
   }
}
