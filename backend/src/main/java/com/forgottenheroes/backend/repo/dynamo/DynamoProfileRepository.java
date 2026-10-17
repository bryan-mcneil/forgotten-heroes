package com.forgottenheroes.backend.repo.dynamo;

import com.forgottenheroes.backend.repo.Profile;
import com.forgottenheroes.backend.repo.ProfileRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Profiles in the PROFILE folder of each USER# drawer: one GetItem to find, one PutItem to save. The
 * bean exists only with {@code app.storage=dynamo}; the table name comes from {@code app.dynamodb.table}.
 */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "dynamo")
public class DynamoProfileRepository implements ProfileRepository {

    private final DynamoDbTable<ProfileItem> table;

    public DynamoProfileRepository(DynamoDbEnhancedClient client, @Value("${app.dynamodb.table}") String tableName) {
        // 1. the translator learns the table's name and ProfileItem's shape, once
        this.table = client.table(tableName, TableSchema.fromBean(ProfileItem.class));
    }

    @Override
    public Optional<Profile> find(String userId) {
        // 2. GetItem: both labels in, one folder out, or null when there is none
        Key key = Key.builder().partitionValue(Keys.user(userId)).sortValue(Keys.PROFILE).build();
        return Optional.ofNullable(table.getItem(key)).map(ProfileItem::toProfile);
    }

    @Override
    public void save(Profile profile) {
        // 3. PutItem: the whole folder written, new or replaced; no insert-or-update question
        table.putItem(ProfileItem.from(profile));
    }
}
