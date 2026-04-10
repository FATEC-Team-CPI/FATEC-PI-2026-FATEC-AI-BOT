package org.acme.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.model.Admin;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import io.quarkus.elytron.security.common.BcryptUtil;
import java.util.Map;

@ApplicationScoped
public class AdminRepository implements IAdminRepository {

    @Inject
    DynamoDbClient dynamoDbClient;

    public Admin findByKeys(String pk, String sk) {
        // Mock para desenvolvimento/teste sem DB
        if ("admin@fatec.sp.gov.br".equals(sk)) {
            Admin mock = new Admin();
            mock.setPk(pk);
            mock.setSk(sk);
            mock.setStatus("active");
            // Mock de senha: "fatec123"
            mock.setPasswordHash(BcryptUtil.bcryptHash("fatec123"));
            return mock;
        }

        // Código real para DynamoDB com GetItemRequest (usar quando DB estiver pronto)
        // GetItemRequest request = GetItemRequest.builder()
        //     .tableName("Admins")
        //     .key(Map.of(
        //         "pk", AttributeValue.builder().s(pk).build(),
        //         "sk", AttributeValue.builder().s(sk).build()
        //     ))
        //     .build();
        // 
        // try {
        //     GetItemResponse response = dynamoDbClient.getItem(request);
        //     if (response.hasItem()) {
        //         Map<String, AttributeValue> item = response.item();
        //         Admin admin = new Admin();
        //         admin.setPk(item.get("pk").s());
        //         admin.setSk(item.get("sk").s());
        //         admin.setPasswordHash(item.get("passwordHash").s());
        //         admin.setStatus(item.get("status").s());
        //         // Adicionar outros campos se necessário (name, role, etc.)
        //         return admin;
        //     }
        // } catch (Exception e) {
        //     // DB não configurado ou erro
        // }
        return null;
    }
}