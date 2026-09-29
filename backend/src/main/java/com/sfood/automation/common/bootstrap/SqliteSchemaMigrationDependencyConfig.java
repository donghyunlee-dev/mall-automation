package com.sfood.automation.common.bootstrap;

import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("!prod")
public class SqliteSchemaMigrationDependencyConfig extends EntityManagerFactoryDependsOnPostProcessor {

    public SqliteSchemaMigrationDependencyConfig() {
        super("sqliteSchemaMigration");
    }
}
