package server.dlm.socket.configuration.database;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "server.dlm.socket.repository.out",
        entityManagerFactoryRef = "outEntityManagerFactory",
        transactionManagerRef = "outTransactionManager"
)
public class OutDatabaseConfig {

    @Bean(name = "outDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.out")
    public DataSource inDataSource() {
        return DataSourceBuilder.create()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }

    @Bean(name = "outEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean outEntityManagerFactory(
            @Qualifier("outDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("server.dlm.socket.entity.out");
        em.setPersistenceUnitName("out");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.put("hibernate.show_sql", false);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "outTransactionManager")
    public PlatformTransactionManager outTransactionManager(
            @Qualifier("outEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
