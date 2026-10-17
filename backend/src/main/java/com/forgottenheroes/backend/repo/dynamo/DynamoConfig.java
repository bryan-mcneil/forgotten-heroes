package com.forgottenheroes.backend.repo.dynamo;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * The two SDK objects every DynamoDB repository shares. With {@code app.dynamodb.endpoint} set,
 * the client talks to the box on your PC with made-up keys; without it, to real AWS.
 */
@Configuration(proxyBeanMethods = false)
public class DynamoConfig {
    @Bean
    DynamoDbClient dynamoDbClient(
            @Value("${app.dynamodb.endpoint:}") String endpoint,
            @Value("${app.dynamodb.region:us-east-1}") String region) {
        // 1. region + the HTTP engine; named explicitly because the SDK ships two
        // others
        var builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(UrlConnectionHttpClient.builder());
        // 2. an address means the box on your PC: any key works there
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }
        return builder.build(); // 3. the phone line
    }

    @Bean
    DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder() // 4. the translator, on that line
                .dynamoDbClient(client)
                .build();
    }
}
