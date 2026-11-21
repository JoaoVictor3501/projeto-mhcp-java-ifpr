package br.edu.ifpr.mcp_server.mcp.files;

/**
 * Lançada pelo {@link SafePath} quando um caminho informado pelo cliente
 * ultrapassa a pasta raiz autorizada.
 *
 * Estende RuntimeException (exceção "não verificada"): quem chama não é
 * obrigado a declarar "throws" nem a usar try/catch. As ferramentas MCP a
 * capturam e a convertem em um resultado de erro para o cliente.
 */
public class PathOutsideRootException extends RuntimeException {
   // identificador de versão da classe, exigido pelo Java para classes
   // serializáveis (toda exceção é); evita o aviso amarelo do compilador
   private static final long serialVersionUID = 1L;

   /**
    * @param mensagem texto explicando qual caminho foi recusado
    */
   public PathOutsideRootException(String mensagem) {
      // repassa a mensagem à classe pai (RuntimeException), que a guarda e a
      // devolve em getMessage()
      super(mensagem);
   }
}
