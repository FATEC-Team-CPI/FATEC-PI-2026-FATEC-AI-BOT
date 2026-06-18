package org.acme.aibot.repository;

import org.acme.aibot.model.Documento;
import java.util.List;

public interface IAIBotRepository {

    /**
     * Salva metadados do arquivo no DB
     */
    void uploadMDnoDynamonDB(Documento documento) throws Exception;

    /**
     * Lista documentos salvos no DynamoDB
     */
    List<Documento> listarDocumentos() throws Exception;

}
