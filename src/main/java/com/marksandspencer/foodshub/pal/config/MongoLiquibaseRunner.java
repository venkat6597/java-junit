package com.marksandspencer.foodshub.pal.config;

import liquibase.Liquibase;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.integration.spring.SpringResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

@Slf4j
public class MongoLiquibaseRunner implements CommandLineRunner, ResourceLoaderAware {

    public final MongoLiquibaseDatabase database;

    protected ResourceLoader resourceLoader;

    @Value("${liquibase.change-log}")
    private String changeLogPath;

    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public MongoLiquibaseRunner(MongoLiquibaseDatabase database) {
        this.database = database;
    }

    public void run(final String... args) throws Exception {
        Liquibase liquiBase = null;
        try {
            liquiBase = new Liquibase(changeLogPath, new SpringResourceAccessor(resourceLoader), database);
            liquiBase.update("");
        } catch (Exception e) {
            log.error("Exception in MongoLiquibaseRunner",e);
        	throw e;
        } finally {
            if (liquiBase != null)
                liquiBase.close();
        }

    }

}
