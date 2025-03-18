package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.self.expense.model.ExpenseData;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

public class ExpenseDataRepositoryExtImpl implements   ExpenseDataRepositoryExt{

    private final ReactiveMongoTemplate mongoTemplate;

    public ExpenseDataRepositoryExtImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }



    @Override
    public Flux<ExpenseData> findAllWithBatch(int batchSize) {
        Query query =  new Query().cursorBatchSize(batchSize);
        return mongoTemplate.find(query, ExpenseData.class);
    }
}
