package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SelfExpenseServiceImpl implements SelfExpenseService{

    private final AnprInfoRepository anprInfoRepository;

    public SelfExpenseServiceImpl(AnprInfoRepository anprInfoRepository) {
        this.anprInfoRepository = anprInfoRepository;
    }

    @Override
    public Mono<ChildResponseDTO> getChildForUserId(String userId, String initiativeId) {

        return anprInfoRepository.findByUserIdAndInitiativeId(userId, initiativeId)
                .map(anprInfo -> {
                    ChildResponseDTO childResponseDTO = new ChildResponseDTO();
                    childResponseDTO.setChildList(anprInfo.getChildList());
                    return childResponseDTO;
                });
    }
}
