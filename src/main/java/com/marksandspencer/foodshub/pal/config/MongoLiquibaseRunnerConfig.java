package com.marksandspencer.foodshub.pal.config;

import liquibase.database.DatabaseFactory;
import liquibase.exception.DatabaseException;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name="liquibase.enabled", havingValue="true")
public class MongoLiquibaseRunnerConfig {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/devsupplierportal}")
    private String connectionString;

    @Bean
    public MongoLiquibaseRunner liquibaseRunner(final MongoLiquibaseDatabase database) {
        return new MongoLiquibaseRunner(database);
    }

    /**
     * @return Database with connection
     * @throws DatabaseException when cannot connect
     */
    @Bean
    public MongoLiquibaseDatabase database() throws DatabaseException {
        return (MongoLiquibaseDatabase) DatabaseFactory.getInstance().openDatabase(connectionString, null, null, null,
                null);
    }

}
