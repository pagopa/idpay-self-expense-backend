package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SelfExpenseServiceImpl implements SelfExpenseService{

    private final AnprInfoRepository anprInfoRepository;

    private final ExceptionMap exceptionMap;

    public SelfExpenseServiceImpl(AnprInfoRepository anprInfoRepository, ExceptionMap exceptionMap) {
        this.anprInfoRepository = anprInfoRepository;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public Mono<ChildResponseDTO> getChildForUserId(String userId, String initiativeId) {

        return anprInfoRepository.findByUserIdAndInitiativeId(userId, initiativeId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                                Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)))
                .map(anprInfo -> {
                    ChildResponseDTO childResponseDTO = new ChildResponseDTO();
                    childResponseDTO.setChildList(anprInfo.getChildList());
                    return childResponseDTO;
                });
    }
}
