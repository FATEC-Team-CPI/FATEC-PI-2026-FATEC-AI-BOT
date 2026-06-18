package org.acme.aibot.repository;
import org.acme.aibot.model.Documento;

public interface IAIBotRepository {

    /**
     * Salva metadados do arquivo no DB
     */
    void uploadMDnoDynamonDB(Documento documento) throws Exception;
    
}
