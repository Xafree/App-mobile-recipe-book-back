package fr.gymgod.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "nutritionEntityManagerFactory", transactionManagerRef = "nutritionTransactionManager", basePackages = {
        "fr.gymgod.common.domain.nutrition" })
public class NutritionDbConfig {

    @Primary
    @Bean(name = "nutritionDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.nutrition")
    public DataSource nutritionDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "nutritionEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nutritionEntityManagerFactory(
            @Qualifier("nutritionDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("fr.gymgod.common.entities.nutrition");
        em.setPersistenceUnitName("nutrition");
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        java.util.HashMap<String, Object> properties = new java.util.HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.jdbc.batch_size", "50");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Primary
    @Bean(name = "nutritionTransactionManager")
    public PlatformTransactionManager nutritionTransactionManager(
            @Qualifier("nutritionEntityManagerFactory") LocalContainerEntityManagerFactoryBean nutritionEntityManagerFactory) {
        return new JpaTransactionManager(nutritionEntityManagerFactory.getObject());
    }
}
