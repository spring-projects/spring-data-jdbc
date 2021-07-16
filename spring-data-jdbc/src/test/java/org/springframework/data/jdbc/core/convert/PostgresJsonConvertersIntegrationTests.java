package org.springframework.data.jdbc.core.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.mapping.JdbcSimpleTypes;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.testing.TestConfiguration;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for PostgreSQL JSON[B] converters.
 * Start this test with -Dspring.profiles.active=postgres
 *
 * @author Nikita Konev
 */
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "postgres")
@ContextConfiguration
@Transactional
@ExtendWith(SpringExtension.class)
public class PostgresJsonConvertersIntegrationTests {

    @Profile("postgres")
    @Configuration
    @Import(TestConfiguration.class)
    @EnableJdbcRepositories(considerNestedRepositories = true,
            includeFilters = @ComponentScan.Filter(value = CustomerRepository.class, type = FilterType.ASSIGNABLE_TYPE))
    static class Config {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Bean
        Class<?> testClass() {
            return PostgresJsonConvertersIntegrationTests.class;
        }

        @WritingConverter
        static class PersonDataWritingConverter extends AbstractPostgresJsonWritingConverter<PersonData> {

            public PersonDataWritingConverter(ObjectMapper objectMapper) {
                super(objectMapper, true);
            }
        }

        @ReadingConverter
        static class PersonDataReadingConverter extends AbstractPostgresJsonReadingConverter<PersonData> {
            public PersonDataReadingConverter(ObjectMapper objectMapper) {
                super(objectMapper, PersonData.class);
            }
        }

        @WritingConverter
        static class SessionDataWritingConverter extends AbstractPostgresJsonWritingConverter<SessionData> {
            public SessionDataWritingConverter(ObjectMapper objectMapper) {
                super(objectMapper, true);
            }
        }

        @ReadingConverter
        static class SessionDataReadingConverter extends AbstractPostgresJsonReadingConverter<SessionData> {
            public SessionDataReadingConverter(ObjectMapper objectMapper) {
                super(objectMapper, SessionData.class);
            }
        }

        private List<Object> storeConverters(Dialect dialect) {

            List<Object> converters = new ArrayList<>();
            converters.addAll(dialect.getConverters());
            converters.addAll(JdbcCustomConversions.storeConverters());
            return converters;
        }

        protected List<?> userConverters() {
            final List<Converter> list = new ArrayList<>();
            list.add(new PersonDataWritingConverter(objectMapper));
            list.add(new PersonDataReadingConverter(objectMapper));
            list.add(new SessionDataWritingConverter(objectMapper));
            list.add(new SessionDataReadingConverter(objectMapper));
            return list;
        }

        @Primary
        @Bean
        CustomConversions jdbcCustomConversions(Dialect dialect) {

            SimpleTypeHolder simpleTypeHolder = dialect.simpleTypes().isEmpty() ? JdbcSimpleTypes.HOLDER
                    : new SimpleTypeHolder(dialect.simpleTypes(), JdbcSimpleTypes.HOLDER);

            return new JdbcCustomConversions(CustomConversions.StoreConversions.of(simpleTypeHolder, storeConverters(dialect)),
                    userConverters());
        }

    }

    @Data
    @AllArgsConstructor
    @Table("customers")
    public static class Customer {

        @Id
        private Long id;
        private String name;
        private PersonData personData;
        private SessionData sessionData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonData {
        private int age;
        private String petName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionData {
        private String token;
        private Long ttl;
    }

    interface CustomerRepository extends CrudRepository<Customer, Long> {

    }

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void testSaveAndGet() {
        final Customer saved = customerRepository.save(new Customer(null, "John Smith", new PersonData(20, "Rex"), new SessionData("j.w.t", 3600L)));
        assertThat(saved.getId()).isNotZero();
        final Optional<Customer> byId = customerRepository.findById(saved.getId());
        assertThat(byId.isPresent()).isTrue();
        final Customer foundCustomer = byId.get();
        assertThat(foundCustomer.getName()).isEqualTo("John Smith");
        assertThat(foundCustomer.getPersonData()).isNotNull();
        assertThat(foundCustomer.getPersonData().getAge()).isEqualTo(20);
        assertThat(foundCustomer.getPersonData().getPetName()).isEqualTo("Rex");
        assertThat(foundCustomer.getSessionData()).isNotNull();
        assertThat(foundCustomer.getSessionData().getToken()).isEqualTo("j.w.t");
        assertThat(foundCustomer.getSessionData().getTtl()).isEqualTo(3600L);
    }

    @Test
    void testSaveAndGetWithNull() {
        final Customer saved = customerRepository.save(new Customer(null, "Adam Smith", new PersonData(30, "Casper"), null));
        assertThat(saved.getId()).isNotZero();
        final Optional<Customer> byId = customerRepository.findById(saved.getId());
        assertThat(byId.isPresent()).isTrue();
        final Customer foundCustomer = byId.get();
        assertThat(foundCustomer.getName()).isEqualTo("Adam Smith");
        assertThat(foundCustomer.getPersonData()).isNotNull();
        assertThat(foundCustomer.getPersonData().getAge()).isEqualTo(30);
        assertThat(foundCustomer.getPersonData().getPetName()).isEqualTo("Casper");
        assertThat(foundCustomer.getSessionData()).isNull();
    }

}
