package server.dlm.socket.configuration.datasource;

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
        basePackages = "server.dlm.socket.repository.in",
        entityManagerFactoryRef = "inEntityManagerFactory",
        transactionManagerRef = "inTransactionManager"
)
public class InDataConfig {

    @Bean(name = "inDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.in")
    public DataSource inDataSource() {
        return DataSourceBuilder.create()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }

    @Bean(name = "inEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean inEntityManagerFactory(
            @Qualifier("inDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("server.dlm.socket.entity.in");
        em.setPersistenceUnitName("in");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.put("hibernate.show_sql", false);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "inTransactionManager")
    public PlatformTransactionManager inTransactionManager(
            @Qualifier("inEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
