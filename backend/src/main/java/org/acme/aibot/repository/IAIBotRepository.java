package org.acme.aibot.repository;

import org.acme.aibot.model.Documento;
import java.util.List;

public interface IAIBotRepository {

    /**
     * Lista documentos salvos no DynamoDB
     */
    List<Documento> listarDocumentos() throws Exception;

}
