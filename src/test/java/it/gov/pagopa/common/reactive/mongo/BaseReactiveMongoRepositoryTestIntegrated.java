package it.gov.pagopa.common.reactive.mongo;

import it.gov.pagopa.common.mongo.MongoTestIntegrated;
import it.gov.pagopa.self.expense.model.AnprInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@MongoTestIntegrated
class BaseReactiveMongoRepositoryTestIntegrated extends BaseReactiveMongoRepositoryIntegrationTest {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;


    @Override
    @Test
    void testFindById() {
        super.testFindById();

        org.bson.Document result = mongoTemplate.executeCommand(new org.bson.Document("getLastRequestStatistics", 1)).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("find", result.get("CommandName"));
        double ru = (double) result.get("RequestCharge");
        Assertions.assertTrue(ru <= 4.0, "Unexpected RU consumed! " + ru);
    }

    @Test
    void testFindByField() {
        // Supponiamo che tu abbia un'entità chiamata YourEntity con un campo "name"
        String expectedFamily = "Test_Family_1";

        // Creare un'istanza di YourEntity e salvarla nel database
        AnprInfo entity = new AnprInfo();
        entity.setUserId("test1");
        entity.setFamilyId(expectedFamily);
        entity.setInitiativeId("initId");
        mongoTemplate.save(entity).block();

        // Creare una query per trovare l'entità per nome
        Query query = new Query();
        query.addCriteria(Criteria.where("familyId").is(expectedFamily));

        // Eseguire la query
        Mono<AnprInfo> resultMono = mongoTemplate.findOne(query, AnprInfo.class);

        // Verificare il risultato
        AnprInfo result = resultMono.block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedFamily, result.getFamilyId());

        mongoTemplate.remove(query, AnprInfo.class).block();

        // Verificare che l'oggetto sia stato rimosso
        AnprInfo deletedResult = mongoTemplate.findOne(query, AnprInfo.class).block();
        Assertions.assertNull(deletedResult, "L'oggetto non è stato rimosso dal database.");
    }

}
